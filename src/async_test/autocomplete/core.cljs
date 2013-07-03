(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer]]
            [clojure.string :as string]
            [async-test.autocomplete.utils
             :refer [key-chan by-id split-chan set-class
                     clear-class timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]
                   [async-test.autocomplete.macros :refer [go-loop]]))

(defn fetch [value]
  (let [c (chan)]
    (go
      (println "get data" value)
      (<! (timeout (rand-int 300)))
      (>! c :data)
      (close! c))
    c))

(defn no-input? [e input-el]
  (let [code (.-keyCode e)]
    (and (= code 8) (string/blank? (.-value input-el)))))

(defn autocompleter* [c input-el ac-el]
  (let [ac (chan)]
    (clear-class ac-el)
    (go
      (loop [start (js/Date.)]
        (let [e (<! c)]
          (cond
            (no-input? e input-el)
            (do (set-class ac-el "hidden")
              (close! ac))
            
            (>= (- (js/Date.) start) 500)
            (do (<! (fetch (.-value input-el)))
              (recur (js/Date.)))

            :else
            (recur start)))))
    ac))

(defn autocompleter [input-el ac-el]
  (let [kc (key-chan (chan (sliding-buffer 1)) input-el "keyup")
        [kc'] (split-chan kc 1)]
    (go-loop
      (<! kc)
      (when (pos? (alength (.-value input-el)))
        (<! (autocompleter* kc' input-el ac-el))))))

(autocompleter
  (by-id "input")
  (by-id "completions"))
