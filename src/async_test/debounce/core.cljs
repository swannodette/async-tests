(ns async-test.throttle.core
  (:require [cljs.core.async :refer [chan close! sliding-buffer]]
            [clojure.string :as string])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alts!]]))

(def c (chan (sliding-buffer 1)))
(def loc-div (.getElementById js/document "location"))

(.addEventListener js/window "mousemove"
  (fn [e]
    (go
      (>! c [(.-x e) (.-y e)]))))

(defn debounce [c ms]
  (let [c' (chan)]
    (go
      (loop [start nil loc (<! c)]
        (if (nil? start)
          (do
            (>! c' loc)
            (recur (js/Date.)))
          (let [loc (<! c)]
            (if (>= (- (js/Date.) start) ms)
              (recur nil loc)
              (recur (js/Date.) loc))))))
    c'))

(def debounced (debounce c 1000))

(go
  (while true
    (let [loc (<! debounced)]
      (aset loc-div "innerHTML" (string/join ", " loc)))))
