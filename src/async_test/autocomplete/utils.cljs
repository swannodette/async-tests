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
  ([type] (mouse-chan (chan (sliding-buffer 1)) type))
  ([c type] (mouse-chan c js/window type))
  ([c el type]
    (.addEventListener el type #(put! c %))))

(defn key-chan
  ([type] (key-chan (chan (sliding-buffer 1)) type))
  ([c type] (key-chan c js/window type))
  ([c el type]
    (.addEventListener el type #(put! c %))))
