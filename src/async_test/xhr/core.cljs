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


(defprotocol Cancel
  (cancel [this]))

(defprotocol Chain
  (chain [this ok-f] [this ok-f err-f])
  (chaincat [this ok-f]))

(defprotocol ReadError
  (error! [this f]))

(declare ->Request)

(defrecord Request [req data err]
  Cancel
  (cancel [_] (cancel req))

  Chain
  (chain [_ ok-f] (->Request req (chan/map ok-f data) err))
  (chain [_ ok-f err-f] (->Request req
                                   (chan/map ok-f data)
                                   (chan/map err-f err)))
  (chaincat [_ f] (->Request req (chan/mapcat f data) err))

  ReadError
  (error! [this f]
    (async/take! err f))

  async-proto/ReadPort
  (take! [this f]
    (async/take! data (async-proto/commit f)))

  async-proto/Channel
  (close! [this]
    (async/close! err)
    (async/close! data)))

(def method-map {:get "GET" :put "PUT" :post "POST" :patch "PATCH" :delete "DELETE"})

(def ^:private
  *xhr-manager*
  (goog.net.XhrManager. nil (js-obj "accept" "text/edn") nil nil 5000))

(defn- read-response [response]
  (let [body (.getResponseText response)]
    (cond
      (empty? body) nil
      (string? body) (js->clj (.parse js/JSON body))
      :else body)))

(defn- handle-response [body err]
  (fn [e]
    (if (.isSuccess e/target)
      (put! body (read-response e/target))
      (put! err (or (read-response e/target) "error")))
    (async/close! body)
    (async/close! err)))

(defn- map->obj [m]
  (reduce (fn [obj [k v]] (aset obj (name k) v) obj)
            (js-obj)
            m))

(defn request-body
  "Asynchronously make a network request for the resource at url.  The
   entry for `:event` contains an instance of the `goog.net.XhrManager.Event`.

   Other allowable keyword arguments are `:method`, `:content`, `:headers`,
   `:priority`, `:retries`, and `:timeout`. `:method` defaults to \"GET\" and `:retries`
   defaults to `0`. `:timout` may be a number of milliseconds or a timeout channel.

   Returns a cancelable Request object with separate data and err channels"
  [url & {:keys [method body headers priority retries id timeout]
          :or   {method   :get
                 retries  0}}]
  (let [body (chan 1) err (chan 1)]
    (when-let [req (.send *xhr-manager*
                        id
                        url
                        (method-map method)
                        body
                        (map->obj headers)
                        priority
                        (handle-response body err)
                        retries)]
      (when timeout
        (let [timeout (if (number? timeout) (async/timeout timeout) timeout)]
          (async/take! timeout (fn [] (cancel req))))) ; <- should cause a failure response...?
      (->Request req body err))))

; This technique seems to be quite slow.
(defn request-records [& args]
  (-> (apply request-body args)
      (chaincat (fn [data]
                  (if (sequential? data)
                    data
                    [data])))))

