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

(def IGNORE #{98 16 37 38 39 40 18 20})

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
  [{start :start c :chan blur :blur} input-el ac-el]
  (let [ac (chan)
        [c' c'' c'''] (multiplex c 3)
        no-input      (no-input c' input-el)
        delay         (after-last c''' 300)]
    (go
      (<! start)
      (loop [interval (throttle c'' 500)]
        (let [[v sc] (alts! [blur no-input interval delay])]
          (condp contains? sc
            #{no-input blur}
            (do (set-class ac-el "hidden")
              (do
                (>! ac :next)
                (<! start)
                (recur (throttle c'' 500))))

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
  (let [c (filter-chan
             (complement IGNORE)
             (map-chan
               #(get % "keyCode")
               (:chan (event-chan input-el "keyup"))))
        [kc kc'] (multiplex c 2)
        ctrl {:start (chan)
              :chan kc'
              :blur (:chan (event-chan input-el "blur"))}
        ac (autocompleter* ctrl input-el ac-el)]
    (go-loop
      (<! kc)
      (when (pos? (alength (.-value input-el)))
        (>! (:start ctrl) :go)
        (<! ac)))))

(autocompleter
  (by-id "input")
  (by-id "completions"))

