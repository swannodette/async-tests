(ns ^{:doc "Make network requests.

            Loosely based on:
            https://github.com/brentonashworth/one
              src/lib/cljs/one/browser/remote.cljs"}
  async-test.xhr.core
  (:require [async-test.xhr.channel :as chan]
            goog.net.XhrManager
            [cljs.core.async :as async :refer [<! >! chan put!]]
            [cljs.core.async.impl.protocols :as async-proto])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]))


(defrecord Request [xhr data err]
  async-proto/ReadPort
  (take! [this f]
    (async/take! data (async-proto/commit f)))

  async-proto/Channel
  (close! [this]
    (async/close! err)
    (async/close! data)))

(defn cancel [req]
  (when (.isActive (:xhr req))
    (.abort (:xhr req))))

(defn on-error! [req f]
  (async/take! (:err req) f))

(defn chain
  ([req ok-f]
   (->Request (:xhr req) (chan/map ok-f (:data req)) (:err req)))
  ([req ok-f err-f]
   (->Request (:xhr req)
              (chan/map ok-f (:data req))
              (chan/map err-f (:err req)))))

(defn chain* [req ok-f err-f]
  (let [err (chan 1)
        recover (chan)]
    (on-error! req #(put! err %))
    (->Request (:xhr req)
               (go (alts! [(chan/map (fn [data] (ok-f data err)) (:data req))
                           recover]))
               (chan/map (fn [data] (err-f data recover)) err))))

(defn chaincat [req f]
  (->Request (:xhr req) (chan/mapcat f (:data req)) (:err req)))


; Build Request ----------------------------------------------------------------

(def method-map {:get "GET" :put "PUT" :post "POST" :patch "PATCH" :delete "DELETE"})

(def ^:private
  *xhr-manager*
  (goog.net.XhrManager. nil (js-obj "accept" "text/edn") nil nil 5000))

(defn- handle-response [body err]
  (fn [e]
    (put! (if (.isSuccess e/target) body err)
          (.getResponseText e/target))
    (async/close! body)))

(defn- map->obj [m]
  (reduce (fn [obj [k v]] (aset obj (name k) v) obj)
            (js-obj)
            m))

(defn request
  "Asynchronously make a network request for the resource at url.  The
   entry for `:event` contains an instance of the `goog.net.XhrManager.Event`.

   Other allowable keyword arguments are `:method`, `:content`, `:headers`,
   `:priority`, `:retries`, and `:timeout`. `:method` defaults to \"GET\" and `:retries`
   defaults to `0`. `:timout` may be a number of milliseconds or a timeout channel.

   Returns a cancelable Request object with separate data and err channels"
  [url {:keys [method body headers priority retries id timeout]
        :or   {method   :get
               retries  0}}]
  (let [body (chan 1) err (chan 1)]
    (when-let [xhr (.send *xhr-manager*
                          id
                          url
                          (method-map method)
                          body
                          (map->obj headers)
                          priority
                          (handle-response body err)
                          retries)]
      (let [req (->Request xhr body err)]
        (when timeout
          (let [timeout (if (number? timeout) (async/timeout timeout) timeout)]
            (async/take! timeout (fn [] (cancel req)))))
        req))))

; Process Response

(defn ->json [req]
  (chain* req
          (fn [body err]
            (try
              (js->clj (.parse js/JSON body))
              (catch js/Error e
                (put! err e))))
          (fn [err recover]
            (if (string? err)
              (try
                (js->clj (.parse js/JSON err))
                (catch js/Error e
                  e))
              err))))

