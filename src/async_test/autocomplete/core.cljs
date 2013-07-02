(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async :refer [<! >! chan put!]]
            [async-test.autocomplete.utils
             :refer [timeout mouse-chan key-chan by-id to-char set-html]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]
                   [clojure.core.match.js :refer [match]]))

(def loc-div (by-id "location"))
(def key-div (by-id "key"))

(defn handler [[e c]]
  (match [e]
    [{"x" x "y" y}] (set-html loc-div (string/join ", " [x y]))
    [{"charCode" c}] (set-html key-div (to-char c))
    :else nil))

(go
  (while true
    (handler (alts! [mc kc]))))
