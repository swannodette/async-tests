(ns async-test.mouse.core
  (:require [cljs.core.async :refer [chan sliding-buffer put! alts!]]
            [clojure.string :as string]
            [async-test.utils.helpers :refer [event-chan set-html! by-id]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [cljs.core.match.macros :refer [match]]))
 
(def loc-div (by-id "location"))
(def key-div (by-id "key"))

(def mc (:chan (event-chan js/window "mousemove")))
(def kc (:chan (event-chan js/window "keyup")))
 
(defn handler [[e c]]
  (match [e]
    [{"x" x "y" y}] (set-html! loc-div (str x ", " y))
    [{"keyCode" code}] (set-html! key-div code)
    :else nil))

(go
  (while true
    (handler (alts! [mc kc]) )))

