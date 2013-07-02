(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! dropping-buffer sliding-buffer]]
            [clojure.string :as string]
            [async-test.autocomplete.utils
             :refer [key-chan by-id throttle split-chan set-class
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

(defn cleanup [ac c ac-el]
  (set-class ac-el "hidden")
  (close! ac))

(defn autocompleter* [c input-el ac-el]
  (let [ac (chan)
        [c] (split-chan c 1)
        [c' c''] (split-chan c 2)
        tc (text-chan (throttle c'' 500) input-el)
        killc (chan)]
    (clear-class ac-el)
    (go (loop []
          (let [e (<! c')]
            (if (no-input? e input-el)
              (do
                (close! killc)
                (recur))
              (recur)))))
    (go (loop []
          (let [[v c] (alts! [tc killc])]
            (if (= c killc)
              (cleanup ac c ac-el)
              (if-not (string/blank? v)
                (do
                  (alts! [(fetch v) killc])
                  (recur))
                (recur))))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [kc (key-chan (chan (sliding-buffer 1)) input-el "keyup")
        [kc'] (split-chan kc 1)]
    (go-loop
      (println "OPEN")
      (<! kc)
      (when (pos? (alength (.-value input-el)))
        (<! (autocompleter* kc' input-el ac-el))))))

(autocompleter
  (by-id "input")
  (by-id "completions"))
