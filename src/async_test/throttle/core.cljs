(ns async-test.throttle.core
  (:require [cljs.core.async :refer [chan close! sliding-buffer put!]]
             [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :as m :refer [go alts!]]))

(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

(def c (chan))
(def loc-div (.getElementById js/document "location"))

(.addEventListener js/window "mousemove"
  (fn [e]
    (put! c [(.-x e) (.-y e)])))

(defn throttle
  ([c ms] (throttle (chan) c ms))
  ([c' c ms]
    (go
      (loop [start nil x nil] ;; core.async bug we can't use <! here
        (let [x (<! c)]
          (if (nil? start)
            (do
              (>! c' x)
              (recur (js/Date.) nil))
            (let [x (<! c)]
              (if (>= (- (js/Date.) start) ms)
                (recur nil x)
                (recur start nil)))))))
    c'))

(def throttled (throttle c 500))
 
(go
  (while true
    (let [loc (<! throttled)]
      (aset loc-div "innerHTML" (string/join ", " loc)))))
