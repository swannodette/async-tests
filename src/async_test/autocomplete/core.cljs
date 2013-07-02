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
      (>! c (.-value input-el)))
    c))

(defn fetch [value]
  (let [c (chan)]
    (go
      (println "get data" value)
      (>! c :data)
      (close! c))
    c))

(defn close-all! [cs]
  (doseq [c cs] (close! c)))

(defn no-input? [e input-el]
  (let [code (.-keyCode e)]
    (and (= code 8) (string/blank? (.-value input-el)))))

(defn autocompleter* [c input-el ac-el]
  (let [ac (chan)
        [c' c''] (fan-out c 2)
        tc (text-chan (throttle c'' 500) input-el)]
    (clear-class ac-el)
    (go (loop []
          (let [e (<! c')]
            (println (.-keyCode e))
            (if-not (nil? e)
              (if (no-input? e input-el)
                (do 
                  (set-class ac-el "hidden")
                  (close-all! [ac c])
                  :done)
                (recur))
              :done))))
    (go
      (loop []
        (if (<! (fetch (<! tc)))
          (recur)
          :done)))
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
