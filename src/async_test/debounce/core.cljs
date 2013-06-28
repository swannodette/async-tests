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

(defn debounce [c ms]
  (let [c' (chan)
        closer (fn [c] (fn [] (close! c)))
        id (atom nil)]
    (go
      (loop [tc nil]
        (if (nil? tc)
          (do
            (>! c' (<! c))
            (let [tc (chan)]
              (reset! id (js/setTimeout (closer tc) ms))
              (recur tc)))
          (do
            (js/clearTimeout @id)
            (reset! id (js/setTimeout (closer tc) ms))
            (<! tc)
            (recur nil)))))
    c'))

(def debounced (debounce c 1000))

(go
  (while true
    (let [loc (<! debounced)]
      (aset loc-div "innerHTML" (string/join ", " loc)))))
