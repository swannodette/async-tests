(ns async-test.timers.core
  (:require [cljs.core.async :as async
             :refer [timeout]]
            [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(loop [i 0]
  (if (< i 1000)
    (do
      (go-loop
        (.log js/console (<! (timeout 1000))))
      (recur (inc i)))))

(go-loop
  (.log js/console "500" (<! (timeout 500))))
