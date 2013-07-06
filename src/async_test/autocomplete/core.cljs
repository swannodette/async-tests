(ns async-test.autocomplete.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     after-last map-chan filter-chan fan-in debounce
                     interval-chan uniq-chan timeout-chan throt monitor
                     trans-chan]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))

(def base-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn show-results [el r]
  (clear-class el "")
  (condp = r
    :empty
    (set-html el "")
    :keep
    nil
    (->> (for [x (nth r 1)]
           (str "<li>" x "</li>"))
         (apply str)
         (set-html el))))

(defn completion-chan [prefix]
  (go
   (if (string/blank? prefix)
     :empty
     (or (<! (timeout-chan 500 (jsonp-chan (str base-url prefix))))
         :keep))))

(def input-el (by-id "input"))
(def comp-el (by-id "completions"))

(def channel (->> (:chan (event-chan input-el "keydown"))
                  (map-chan (fn [_] (.trim (.-value input-el))))
                  (uniq-chan)
                  (throt 750)
                  (monitor "throttle")
                  (trans-chan completion-chan)))

(go-loop
 (show-results comp-el (<! channel)))

