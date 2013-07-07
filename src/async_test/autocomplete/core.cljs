(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     after-last map-chan filter-chan fan-in debounce
                     distinct-chan]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(def UP_ARROW 38)
(def DOWN_ARROW 40)

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn show-results [r]
  (let [rs (by-id "completions")
        _  (clear-class rs)
        xs (nth r 1)]
    (->> (for [x xs] (str "<li>" x "</li>"))
      (apply str)
      (set-html rs))))

(defn autocompleter*
  [{c :chan arrows :arrows blur :blur} input-el ac-el]
  (let [ac (chan)
        [c' c''] (multiplex c 2)
        no-input (filter-chan string/blank? c')
        fetch    (throttle (filter-chan #(> (count %) 2) c'') 500)]
    (go
      (loop [cancel false]
        (let [[v sc] (alts! [blur no-input arrows fetch])]
          (condp contains? sc
            #{no-input blur}
            (do (set-class ac-el "hidden")
              (recur true))

            #{arrows}
            (do
              (println v)
              (recur cancel))

            #{fetch}
            (if-not cancel
              (let [r (<! (jsonp-chan (str base-url v)))]
                (show-results r)
                (recur false))
              (recur false))))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [raw   (event-chan input-el "keyup")
        codes (map-chan #(get % "keyCode") (:chan raw))
        [keys' keys''] (multiplex codes 2)
        ctrl {:chan   (distinct-chan
                        (map-chan #(do % (.-value input-el))
                          keys'))
              :arrows (filter-chan #{UP_ARROW DOWN_ARROW} keys'')
              :blur   (:chan (event-chan input-el "blur"))}]
    (autocompleter* ctrl input-el ac-el)))

(autocompleter
  (by-id "input")
  (by-id "completions"))
