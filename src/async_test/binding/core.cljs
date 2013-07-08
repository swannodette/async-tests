(ns async-test.binding.core
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [clojure.string :as string]
            [async-test.utils.helpers
             :refer [event-chan by-id copy-chan set-class throttle
                     clear-class jsonp-chan set-html now multiplex
                     map-chan filter-chan fan-in distinct-chan
                     by-tag-name collection subscribe unsubscribe]])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]
                   [async-test.utils.macros :refer [go-loop]]))


(def cds (collection))

;; interact with database

(let [c (chan)]
  (go
    (>! (:in cds)
      {:op :create
        :val {:title "Soft Machine Vol. 1"
              :artist "Soft Machine"
              :year 1969}})
    (>! (:in cds)
      {:op :create
        :val {:title "Marble Index"
              :artist "Nico"
              :year 1969}})
    (>! (:in cds)
      {:op :create
        :val {:title "Plastic Ono Band"
              :artist "Plastic Ono Band"
              :year 1970}})
    (>! (:in cds)
      {:op :query
       :val #(= (:title %) "Marble Index")
       :out c})
    (println "query result" (<! c))))

;; listen to stream of all db events

(let [stream (subscribe (:events cds) (chan))]
  (go-loop
    (println "EVENT LISTEN:" (<! stream))))

