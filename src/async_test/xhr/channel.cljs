(ns async-test.xhr.channel
  (:refer-clojure :exclude [map mapcat])
  (:require-macros
    [cljs.core.async.macros :refer [go]])
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
