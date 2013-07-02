(ns async-test.mouse.core
  (:require [cljs.core.async :refer [chan sliding-buffer put!]]
            [clojure.string :as string])
  (:require-macros [cljs.core.async.macros :as m :refer [go alts!]]
                   [clojure.core.match.js :refer [match]]))
 
(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

(def mc (chan (sliding-buffer 1)))
(def loc-div (.getElementById js/document "location"))

(def kc (chan (sliding-buffer 1)))
(def key-div (.getElementById js/document "key"))

(.addEventListener js/window "mousemove"
  (fn [e]
    (put! mc
      {:type :mouse
       :loc [(.-x e) (.-y e)]})))

(.addEventListener js/window "keyup"
  (fn [e]
    (put! kc
      {:type :key
       :char (.fromCharCode js/String (.-keyCode e))})))
 
(defn set-html [el s]
  (aset el "innerHTML" s))

(defn handler [[e c]]
  (match [e]
    [{:type :mouse :loc loc}] (set-html loc-div (string/join ", " loc))
    [{:type :key :char char}] (set-html key-div char)
    :else nil))

(go
  (while true
    (handler (alts! [mc kc]) )))

