(ns async-test.throttle.core
  (:require
    [cljs.core.async :refer [chan close! sliding-buffer put!]]
    [clojure.string :as string]
    [async-test.utils.helpers :refer [event-chan throttle interval-chan]])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]
    [async-test.utils.macros :refer [go-loop]]))

(def loc-div (.getElementById js/document "location"))

(def throttled
  (throttle (:chan (event-chan js/window "mousemove")) 1000))
 
(go
  (while true
    (let [e (<! throttled)]
      (aset loc-div "innerHTML" (str (.-x e) ", " (.-y e))))))
