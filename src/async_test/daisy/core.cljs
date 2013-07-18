(ns async-test.daisy
  (:require [cljs.core.async :refer [chan <! >!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn f [left right]
  (go (>! left (inc (<! right)))))

(let [leftmost (chan)
      rightmost (loop [n 1000 left leftmost]
                  (if-not (pos? n)
                    left
                    (let [right (chan)]
                      (f left right)
                      (recur (dec n) right))))]
  (go
    (let [s (js/Date.)]
      (>! rightmost 1)
      (.log js/console (<! leftmost) " elapsed ms: "
        (- (.valueOf (js/Date.)) (.valueOf s))))))
