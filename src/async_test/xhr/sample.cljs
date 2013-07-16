(ns async-test.xhr.sample
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async
             :refer [<! close!]]
            [async-test.xhr.core :as xhr]
            [async-test.xhr.channel :as ch]
            [async-test.utils.helpers
             :refer [event-chan by-id set-html set-class]]))

(defn row-data [row]
  (let [bill (get row "bill")]
    (str "<tr><td>" (get bill "bill_number") "</td>"
         "<td>" (get bill "title") "</td></tr>")))

(defn show-page [clicks req]
  (go
    (let [page (ch/take 9 req)
          html (loop [rows [] row (<! page)]
                 (if row
                   (recur (conj rows (row-data row)) (<! page))
                   (when (seq rows)
                     (when (> 9 (count rows))
                       (set-class (by-id "more") "hidden"))
                     (apply str rows))))]
      (when html
        (set-html (by-id "bills") html)
        (<! clicks)
        (show-page clicks req)))))

(defn init-page []
  (let [button (by-id "more")
        clicks (:chan (event-chan button "click"))
        table (by-id "bills")]
    (go
      (<! clicks)
      (set-html button "Next 9 Rows")
      (show-page clicks (xhr/request-records "/bills.json")))))

(init-page)
