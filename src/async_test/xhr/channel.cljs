(ns async-test.xhr.channel
  (:refer-clojure :exclude [map mapcat take drop])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]])
  (:require [cljs.core.async :refer [<! >! chan close!]]))

(defn map [f c]
  (let [r (chan)]
    (go
      (loop []
        (let [v (<! c)]
          (if (nil? v)
            (close! r)
            (do
              (when-let [v (f v)]
              (>! r v))
              (recur))))))
    r))

(defn mapcat [f c]
  (let [r (chan)]
    (go
      (loop []
        (let [v (<! c)]
          (if (nil? v)
            (close! r)
            (do (doseq [v (f v)]
                  (when-not (nil? v) (>! r v)))
                (recur))))))
    r))

(defn take [limit c]
  (let [r (chan)]
    (go
      (loop [n 0]
        (if (< n limit)
          (let [v (<! c)]
            (if (nil? v)
              (close! r)
              (do (>! r v)
                  (recur (inc n)))))
          (close! r))))
    r))

(defn drop [offset c]
  (go
    (loop [n 0]
      (when (< n offset)
        (let [v (<! c)]
          (if (nil? v)
            (close! c)
            (recur (inc n)))))))
  c)

