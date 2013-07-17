(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     map-chan filter-chan fan-in distinct-chan
                     by-tag-name tag-match index-of]]
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
                items  (by-tag-name list-el "li")]
            (cond
              (= control sc) :done
              (= v ENTER) (do (>! c (nth data selected))
                            (recur selected))
              :else (do (when (number? selected)
                          (clear-class (nth items selected)))
                      (let [n (select items selected v)]
                        (set-class (nth items n) "selected")
                        (recur n)))))))
      {:chan c
       :control control})))

#_(let [el (by-id "test")
      c (selector
           (fan-in
             [(:chan (event-chan el "mouseover"))
              (->> (:chan (event-chan js/window "keydown"))
                (map-chan #(.-keyCode %))
                (filter-chan SELECTOR_KEYS))])
           (by-id "test")
           ["one" "two" "three"])]
  (go-loop
    (.log js/console (<! (:chan c)))))

(let [li-match (tag-match "li")
      el (by-id "test")
      lis (by-tag-name el "li")
      c  (->> (:chan (event-chan el "mouseover"))
           (map-chan
             (fn [e]
               (let [target (.-target e)]
                 (if (li-match target)
                   target
                   (dom/getAncestor target li-match)))))
           (filter-chan identity)
           (distinct-chan)
           (map-chan
             #(index-of lis %)))]
  (go-loop
    (.log js/console (<! c))))

(comment
  #(.toLowerCase (.. % -target -tagName))
  )

(defn autocompleter*
  [{c :chan arrows :arrows blur :blur} input-el ac-el]
  (let [ac (chan)
        [c' c''] (multiplex c 2)
        hide     (fan-in [(filter-chan string/blank? c') blur])
        fetch    (throttle (filter-chan #(> (count %) 2) c'') 500)
        [select arrows] (multiplex arrows
                          [(chan (dropping-buffer 1)) (chan)])]
    (go
      (loop [data nil cancel false]
        (let [[v sc] (alts! [hide select fetch])]
          (condp = sc
            hide
            (do (set-class ac-el "hidden")
              (recur data true))

            select
            (if data
              (let [_ (>! select (now))
                   {selector :chan control :control} (selector arrows ac-el data)
                   [v sc] (alts! [selector hide])]
                (when (= sc selector)
                  (aset input-el "value" v))
                (>! control :exit)
                (set-class ac-el "hidden")
                (<! select)
                (recur data true))
              (recur data true))

            fetch
            (if-not cancel
              (let [res (<! (jsonp-chan (str base-url v)))]
                (show-results res)
                (recur (nth res 1) false))
              (recur data false))))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [raw   (event-chan input-el "keydown")
        codes (map-chan #(.-keyCode %) (:chan raw))
        [keys' keys''] (multiplex codes 2)
        ctrl {:chan   (distinct-chan
                        (map-chan #(do % (.-value input-el))
                          keys'))
              :arrows (filter-chan SELECTOR_KEYS keys'')
              :blur   (:chan (event-chan input-el "blur"))}]
    {:chan (autocompleter* ctrl input-el ac-el)}))

#_(autocompleter
  (by-id "input")
  (by-id "completions"))

