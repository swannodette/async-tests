(ns async-test.throttle.core
  (:require [cljs.core.async :refer [chan close! sliding-buffer put!]]
            [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :as m :refer [go alts!]]))

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

(defn debounce
  ([c ms] (debounce (chan) c ms))
  ([c' c ms]
    (go
      (loop [start nil loc nil] ;; core.async bug we can use <! here
        (if (nil? start)
          (let [loc (<! c)]
            (do
              (>! c' loc)
              (recur (js/Date.) nil)))
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
