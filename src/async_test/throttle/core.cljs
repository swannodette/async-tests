(ns async-test.throttle.core
   (:require [cljs.core.async :refer [chan close! sliding-buffer]]
             [clojure.string :as string])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alts!]]))

(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

(def c (chan (sliding-buffer 1)))
(def loc-div (.getElementById js/document "location"))

(.addEventListener js/window "mousemove"
  (fn [e]
    (put! c [(.-x e) (.-y e)])))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn throttle
  ([c ms] (throttle (chan) c ms))
  ([c' c ms]
    (go
      (while true
        (>! c' (<! c))
        (<! (timeout ms))))
    c'))

(def throttled (throttle c 500))
 
(go
  (while true
    (let [loc (<! throttled)]
      (aset loc-div "innerHTML" (string/join ", " loc)))))
