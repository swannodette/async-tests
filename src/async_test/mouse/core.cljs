(ns async-test.mouse.core
  (:require [cljs.core.async :refer [chan sliding-buffer]]
            [clojure.string :as string])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alts!]]))
 
(def c (chan (sliding-buffer 1)))
(def loc-div (.getElementById js/document "location"))

(.addEventListener js/window "mousemove"
  (fn [e]
    (go
      (>! c [(.-x e) (.-y e)]))))
 
(go
  (while true
    (let [[loc c] (alts! [c] :default [nil nil])]
      (when c
        (aset loc-div "innerHTML" (string/join ", " loc))))))
