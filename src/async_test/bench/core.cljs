(ns async-test.bench.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan put! take! sliding-buffer]]
            [cljs.core.async.impl.dispatch])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]))

(def dispatches (atom 0))

(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

(defn pipe [in out]
  (go (>! out (<! in))))

(def c0 (chan (sliding-buffer 1)))
(def c1 (chan (sliding-buffer 1)))
(def c2 (chan (sliding-buffer 1)))
(def c3 (chan (sliding-buffer 1)))
(def c4 (chan (sliding-buffer 1)))
(def c5 (chan (sliding-buffer 1)))
(def c6 (chan (sliding-buffer 1)))
(def c7 (chan (sliding-buffer 1)))

(pipe c0 c1)
(pipe c1 c2)
(pipe c2 c3)
(pipe c3 c4)
(pipe c4 c5)
(pipe c5 c6)
(pipe c6 c7)

(set! cljs.core.async.impl.dispatch.run
  (fn [f]
    (swap! dispatches inc)
    (js/setTimeout f 0)))

(def s (js/Date.))

(go
  (loop [i 0]
    (if (>= i 250000)
      (println "Done, elapsed" (- (js/Date.) s) "dispatches" @dispatches)
      (do
        (>! c0 :foo)
        (recur (inc i))))))

(comment
  (put! c0 :foo)
  (take! c7 (fn [v] (println "got" v)))
  )
