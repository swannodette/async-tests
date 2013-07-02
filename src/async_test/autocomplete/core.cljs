(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan dropping-buffer]]
            [clojure.string :as string]
            [async-test.autocomplete.utils
             :refer [key-chan by-id  throttle fan-out]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]
                   [clojure.core.match.js :refer [match]]
                   [async-test.autocomplete.macros :refer [go-loop]]))

(def input-el (by-id "input"))
(def kc (key-chan input-el "keyup"))

(defn text-chan
  ([cc input-el] (text-chan (chan) cc input-el))
  ([c cc el]
    (go-loop
      (<! cc)
      (>! c {:input (.-value input-el)}))
    c))

(defn handler [[e c]]
  (match [e]
    [{:input input}] (.log js/console input)
    [{"keyCode" c}]  (.log js/console c)
    :else nil))

(let [[kc' kc''] (fan-out kc 2)
       tc        (text-chan (throttle kc'' 300) input-el)]
  (go
    (while true
      (handler (alts! [kc' tc])))))
