(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer timeout]]
            [clojure.string :as string]
            [async-test.autocomplete.utils
             :refer [key-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]
                   [async-test.autocomplete.macros :refer [go-loop]]))

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

(defn autocompleter* [c input-el ac-el]
  (let [ac  (chan)
        c'  (copy-chan c)
        thc (throttle c' 500)]
    (go
      (loop [tmc nil]
        (let [[e sc] (if tmc
                       (alts! [c thc tmc])
                       (alts! [c thc]))]
          (cond
            (and (= sc c) (no-input? e input-el))
            (do (set-class ac-el "hidden")
              (close! ac))

            (= sc c) (recur (timeout 300))
          
            (get #{thc tmc} sc)
            (let [r (<! (jsonp-chan (str base-url (.-value input-el))))]
              (show-results r)
              (recur nil))

            :else (recur tmc)))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [kc  (key-chan input-el "keyup")
        kc' (copy-chan kc)]
    (go-loop
      (<! kc)
      (when (pos? (alength (.-value input-el)))
        (<! (autocompleter* kc' input-el ac-el))))))

(autocompleter
  (by-id "input")
  (by-id "completions"))
