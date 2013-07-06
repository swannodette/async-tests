(ns async-test.timeout.core
  (:require [cljs.core.async :refer [chan close! timeout]]
            [async-test.utils.helpers])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]))
 
(go
  (<! (timeout 1000))
  (.log js/console "Hello")
  (<! (timeout 1000))
  (.log js/console "async")
  (<! (timeout 1000))
  (.log js/console "world!"))
