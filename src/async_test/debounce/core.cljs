(ns async-test.throttle.core
  (:require [cljs.core.async :refer [chan close! sliding-buffer put!]]
            [clojure.string :as string]
            [async-test.utils.helpers :refer [event-chan debounce]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alts!]]))

(def loc-div (.getElementById js/document "location"))

(def debounced
  (debounce (:chan (event-chan js/window "mousemove")) 1000))

(go
  (while true
    (let [e (<! (:chan debounced))]
      (aset loc-div "innerHTML" (str (.-x e) ", " (.-y e))))))
