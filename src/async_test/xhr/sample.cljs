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

(defn pages [req]
  (ch/mapcat #(partition-all 9 %) req))

(defn show-page [clicks pages]
  (go
    (let [page (<! pages)
          rows (for [row page]
                 (row-data row))
          html (apply str rows)]
      (when (> 9 (count rows))
        (set-class (by-id "more") "hidden"))
      (when (seq rows)
        (set-html (by-id "bills") html)
        (<! clicks)
        (show-page clicks pages)))))

(defn init-page []
  (let [button (by-id "more")
        clicks (:chan (event-chan button "click"))
        table (by-id "bills")]
    (set-html button "Show 9 Rows")
    (go
      (<! clicks)
      (set-html button "Next 9 Rows")
      (let [req (xhr/->json (xhr/request "/xhr.json" {}))]
        (xhr/error! req (fn [e]
                          (set-html table (str "<tr><td>Request Error:" e "</td></tr>"))
                          (init-page)))
        (show-page clicks (pages req))))))

(init-page)
