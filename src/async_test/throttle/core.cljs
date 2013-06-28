(ns async-test.throttle.core
  (:require [cljs.core.async :refer [chan close!]]
            [clojure.string :as string])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alts!]]))

(def c (chan))
(def loc-div (.getElementById js/document "location"))

(.addEventListener js/window "mousemove"
  (fn [e]
    (go
      (>! c [(.-x e) (.-y e)]))))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn throttle [c ms]
  (let [c' (chan)]
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
