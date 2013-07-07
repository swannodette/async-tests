(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     map-chan filter-chan fan-in distinct-chan
                     by-tag-name]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(def ENTER 13)
(def UP_ARROW 38)
(def DOWN_ARROW 40)
(def SELECTOR_KEYS #{ENTER UP_ARROW DOWN_ARROW})

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn show-results [r]
  (let [rs (by-id "completions")
        _  (clear-class rs)
        xs (nth r 1)]
    (->> (for [x xs] (str "<li>" x "</li>"))
      (apply str)
      (set-html rs))))

(defn select [items idx key]
  (if (= idx ::none)
    (condp = key
      UP_ARROW (dec (count items))
      DOWN_ARROW 0)
    (mod (({UP_ARROW dec DOWN_ARROW inc} key) idx)
      (count items))))

(defn selector
  ([key-chan list-el data]
    (selector (chan) key-chan list-el data))
  ([c key-chan list-el data]
    (let [control (chan)]
      (go
        (loop [selected ::none]
          (let [[v sc] (alts! [key-chan control])
                items  (by-tag-name list-el "li")]
            (cond
              (and (= control sc) (= v :clear))
              (do
                (when (number? selected)
                  (clear-class (nth items selected)))
                (recur ::none))
            
              (= v ENTER)
              (do
                (>! c (nth data selected))
                (recur selected))

              :else
              (do
                (when (number? selected)
                  (clear-class (nth items selected)))
                (let [n (select items selected v)]
                  (set-class (nth items n) "selected")
                  (recur n)))))))
      {:chan c
       :control control})))

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

            #{fetch}
            (if-not cancel
              (let [r (<! (jsonp-chan (str base-url v)))]
                (show-results r)
                (recur false))
              (recur false))))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [raw   (event-chan input-el "keyup")
        codes (map-chan #(.-keyCode %) (:chan raw))
        [keys' keys''] (multiplex codes 2)
        ctrl {:chan   (distinct-chan
                        (map-chan #(do % (.-value input-el))
                          keys'))
              :arrows (filter-chan SELECTOR_KEYS keys'')
              :blur   (:chan (event-chan input-el "blur"))}]
    (autocompleter* ctrl input-el ac-el)))

#_(autocompleter
  (by-id "input")
  (by-id "completions"))

(let [c (:chan
          (selector
            (filter-chan SELECTOR_KEYS
              (map-chan #(.-keyCode %)
                (:chan (event-chan js/window "keyup"))))
            (by-id "selector-test")
            ["one" "two" "three"]))]
  (go-loop
    (println (<! c))))

