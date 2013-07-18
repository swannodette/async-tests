(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers :as h]
            [async-test.utils.reactive :as r]
            [goog.dom :as dom])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(def ENTER 13)
(def UP_ARROW 38)
(def DOWN_ARROW 40)
(def SELECTOR_KEYS #{ENTER UP_ARROW DOWN_ARROW})

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn show-results [r]
  (let [rs (h/by-id "completions")
        _  (h/clear-class rs)
        xs (nth r 1)]
    (->> (for [x xs] (str "<li>" x "</li>"))
      (apply str)
      (h/set-html rs))))

(defn select [items idx key]
  (if (= idx ::none)
    (condp = key
      UP_ARROW (dec (count items))
      DOWN_ARROW 0)
    (mod (({UP_ARROW dec DOWN_ARROW inc} key) idx)
      (count items))))

;; fan-in mouse & keys, get message :inc, :dec, :set
;; only need it when selector appears

(defn selector
  ([key-chan list-el data]
    (selector (chan) key-chan list-el data))
  ([c key-chan list-el data]
    (let [control (chan)]
      (go
        (loop [selected ::none]
          (let [[v sc] (alts! [key-chan control])
                items  (h/by-tag-name list-el "li")]
            (cond
              (= control sc) :done
              (= v ENTER) (do (>! c (nth data selected))
                            (recur selected))
              :else (do (when (number? selected)
                          (h/clear-class (nth items selected)))
                      (let [n (select items selected v)]
                        (h/set-class (nth items n) "selected")
                        (recur n)))))))
      {:chan c
       :control control})))

(let [el (h/by-id "test")
      c  (:chan (r/hover-chan el "li"))]
  (go-loop
    (.log js/console (<! c))))

(defn autocompleter*
  [{c :chan arrows :arrows blur :blur} input-el ac-el]
  (let [ac (chan)
        [c' c''] (r/multiplex c 2)
        hide     (r/fan-in [(r/filter string/blank? c') blur])
        fetch    (r/throttle (r/filter #(> (count %) 2) c'') 500)
        [select arrows] (r/multiplex arrows
                          [(chan (dropping-buffer 1)) (chan)])]
    (go
      (loop [data nil cancel false]
        (let [[v sc] (alts! [hide select fetch])]
          (condp = sc
            hide
            (do (h/set-class ac-el "hidden")
              (recur data true))

            select
            (if data
              (let [_ (>! select (r/now))
                   {selector :chan control :control} (selector arrows ac-el data)
                   [v sc] (alts! [selector hide])]
                (when (= sc selector)
                  (aset input-el "value" v))
                (>! control :exit)
                (h/set-class ac-el "hidden")
                (<! select)
                (recur data true))
              (recur data true))

            fetch
            (if-not cancel
              (let [res (<! (r/jsonp (str base-url v)))]
                (show-results res)
                (recur (nth res 1) false))
              (recur data false))))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [raw   (r/events input-el "keydown")
        codes (r/map #(.-keyCode %) (:chan raw))
        [keys' keys''] (r/multiplex codes 2)
        ctrl {:chan   (r/distinct
                        (r/map #(do % (.-value input-el))
                          keys'))
              :arrows (r/filter SELECTOR_KEYS keys'')
              :blur   (:chan (r/events input-el "blur"))}]
    {:chan (autocompleter* ctrl input-el ac-el)}))

#_(autocompleter
  (by-id "input")
  (by-id "completions"))

