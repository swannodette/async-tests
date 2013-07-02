(ns async-test.autocomplete.utils
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put!]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

(extend-type object
  ILookup
  (-lookup [coll k]
    (-lookup coll k nil))
  (-lookup [coll k not-found]
    (let [prop (str k)]
      (if (.hasOwnProperty coll prop)
        (aget coll prop)
        not-found))))

(defn by-id [id] (.getElementById js/document id))

(defn set-html [el s]
  (aset el "innerHTML" s))

(defn to-char [code]
  (.fromCharCode js/String code))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn mouse-chan
  ([type] (mouse-chan js/window type))
  ([el type] (mouse-chan (chan (sliding-buffer 1)) el type))
  ([c el type]
    (.addEventListener el type #(put! c %))
    c))

(defn key-chan
  ([type] (key-chan js/window type))
  ([el type] (key-chan (chan (sliding-buffer 1)) el type))
  ([c el type]
    (.addEventListener el type #(put! c %))
    c))

(defn throttle
  ([c ms] (throttle (chan) c ms))
  ([c' c ms]
    (go
      (while true
        (>! c' (<! c))
        (<! (timeout ms))))
    c'))

(defn fan-in [ins]
  (let [c (chan)]
    (go (while true
          (let [[x] (alts! ins)]
            (>! c x))))
    c))

(defn fan-out [in cs-or-n]
  (let [cs (if (number? cs-or-n)
             (repeatedly cs-or-n chan)
             cs-or-n)]
    (go (while true
          (let [x (<! in)
                outs (map #(vector % x) cs)]
            (alts! outs))))
    cs))
