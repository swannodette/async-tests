(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     after-last map-chan filter-chan]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn no-input? [e input-el]
  (and (= (.-keyCode e) 8)
       (string/blank? (.-value input-el))))

(defn show-results [r]
  (let [rs (by-id "completions")
        _  (clear-class rs)
        xs (nth r 1)]
    (->> (for [x xs] (str "<li>" x "</li>"))
      (apply str)
      (set-html rs))))

(defn autocompleter* [{c :key} input-el ac-el]
  (let [ac (chan)
        [c tc dc] (map #(%1 %2)
                    [identity #(throttle % 500) #(after-last % 300)]
                    (multiplex c 3))]
    (go-loop
      (let [[v sc] (alts! [c tc dc])]
        (condp contains? sc
          #{c}
          (if (no-input? v input-el)
            (do (set-class ac-el "hidden")
              (close! ac)))
          
          #{tc dc}
          (let [r (<! (jsonp-chan (str base-url (.-value input-el))))]
            (show-results r)))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [kc (:chan (event-chan input-el "keyup"))        
        chans {:key (copy-chan kc)}]
    (go-loop
      (<! kc)
      (when (pos? (alength (.-value input-el)))
        (<! (autocompleter* chans input-el ac-el))))))

(autocompleter
  (by-id "input")
  (by-id "completions"))
