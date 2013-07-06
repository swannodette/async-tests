(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     after-last map-chan filter-chan fan-in]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn show-results [r]
  (let [rs (by-id "completions")
        _  (clear-class rs)
        xs (nth r 1)]
    (->> (for [x xs] (str "<li>" x "</li>"))
      (apply str)
      (set-html rs))))

(defn no-input [c el] 
  (filter-chan string/blank?
    (map-chan #(do % (.-value el)) c)))

(defn autocompleter*
  [{c :chan blur :blur} input-el ac-el]
  (let [ac            (chan)
        [c' c'' c'''] (multiplex c 3)
        no-input      (no-input c' input-el)
        delay         (after-last c''' 300)]
    (go
      (loop [interval (throttle c'' 500)]
        (let [[v sc] (alts! [blur no-input interval delay])]
          (condp contains? sc
            #{no-input blur}
            (do (set-class ac-el "hidden")
              (close! ac))
          
            #{interval delay}
            (let [r (<! (jsonp-chan (str base-url (.-value input-el))))]
              (show-results r)
              (recur
                (if (= sc delay)
                  (do
                    (close! interval)
                    (throttle c'' 500))
                  interval)))))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [kc (:chan (event-chan input-el "keyup"))
        chans {:chan (copy-chan kc)
               :blur (:chan (event-chan input-el "blur"))}]
    (go-loop
      (<! kc)
      (when (pos? (alength (.-value input-el)))
        (<! (autocompleter* chans input-el ac-el))))))

(autocompleter
  (by-id "input")
  (by-id "completions"))

