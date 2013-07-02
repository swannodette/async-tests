(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close!]]
            [clojure.string :as string]
            [async-test.autocomplete.utils
             :refer [key-chan by-id throttle fan-out set-class
                     clear-class]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]
                   [clojure.core.match.js :refer [match]]
                   [async-test.autocomplete.macros :refer [go-loop]]))

(defn text-chan
  ([cc input-el] (text-chan (chan) cc input-el))
  ([c cc input-el]
    (go-loop
      (<! cc)
      (>! c {:input (.-value input-el)}))
    c))

(defn handler [[e c] input-el cc]
  (match [e]
    [{:input input}] {:state :fetch :value input}
    [{"keyCode" 46}] {:state :done}
    [{"keyCode" c}]  (.log js/console c)
    :else nil))

(defn fetch [value]
  (let [c (chan)]
    (go
      (println "get data" value)
      (>! c :data)
      (close! c))
    c))

(defn autocompleter* [c input-el ac-el]
  (let [ac (chan)
        [c' c''] (fan-out c 2)
        tc (text-chan (throttle c'' 300) input-el)]
    (go
      (clear-class ac-el)
      (loop []
        (let [m (handler (alts! [c' tc]))
              st (get m :state)]
          (condp = st
            :done (do
                    (set-class ac-el "hidden")
                    (close! ac))
            :fetch (let [xs (<! (fetch (get m :value)))]
                     (recur))
            (recur)))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [kc (key-chan input-el "keyup")
        [kc' kc''] (fan-out kc 2)]
    (go-loop
      (<! kc')
      (when (pos? (alength (.-value input-el)))
        (<! (autocompleter* kc'' input-el ac-el))))))

(autocompleter
  (by-id "input")
  (by-id "completions"))
