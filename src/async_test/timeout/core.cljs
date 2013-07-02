(ns async-test.timeout.core
  (:require [cljs.core.async :refer [chan close!]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]))
 
(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(def empty [])
 
(go
  (<! (timeout 1000))
  (.log js/console "Hello")
  (<! (timeout 1000))
  (.log js/console "async")
  (<! (timeout 1000))
  (.log js/console "world!"))
