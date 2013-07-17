(ns async-test.utils.helpers
  (:require
    [cljs.core.async :as async
     :refer [<! >! chan close! sliding-buffer dropping-buffer
             put! timeout]]
    [cljs.core.async.impl.protocols :as proto]
    [goog.net.Jsonp]
    [goog.Uri]
    [goog.dom :as dom])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt!]]
    [async-test.utils.macros :refer [go-loop]]))

;; =============================================================================
;; Printing

(defn js-print [& args]
  (if (js* "typeof console != 'undefined'")
    (.log js/console (apply str args))
    (js/print (apply str args))))

(set! *print-fn* js-print)

;; =============================================================================
;; Pattern matching support

(extend-type object
  ILookup
  (-lookup [coll k]
    (-lookup coll k nil))
  (-lookup [coll k not-found]
    (if (.hasOwnProperty coll k)
      (aget coll k)
      not-found)))

;; =============================================================================
;; Utilities

(defn now []
  (.valueOf (js/Date.)))

(defn by-id [id] (dom/getElement id))

(defn set-html [el s]
  (aset el "innerHTML" s))

(defn to-char [code]
  (.fromCharCode js/String code))

(defn set-class [el name]
  (set! (.-className el) name))

(defn clear-class [el name]
  (set! (.-className el) ""))

(defn by-tag-name [el tag]
  (.getElementsByTagName el tag))

(defn tag-match [tag]
  (fn [el]
    (when-let [tag-name (.-tagName el)]
      (= tag (.toLowerCase tag-name)))))

(defn index-of [node-list el]
  (loop [i 0]
    (if (< i (alength node-list))
      (if (identical? (aget node-list i) el)
        i
        (recur (inc i)))
      -1)))

;; DO NOT DO THIS IN LIBRARIES

(extend-type default
  ICounted
  (-count [coll]
    (if (instance? js/NodeList coll)
      (alength coll)
      (accumulating-seq-count coll)))
  IIndexed
  (-nth
    ([coll n]
      (-nth coll n nil))
    ([coll n not-found]
      (if (instance? js/NodeList coll)
        (if (< n (count coll))
          (aget coll n)
          (throw (js/Error. "NodeList access out of bounds")))
        (linear-traversal-nth coll (.floor js/Math n) not-found)))))

;; =============================================================================
;; Channels

(defn put-all! [cs x]
  (doseq [c cs]
    (put! c x)))

(defn multiplex [in cs-or-n]
  (let [cs (if (number? cs-or-n)
             (repeatedly cs-or-n chan)
             cs-or-n)]
    (go (loop []
          (let [x (<! in)]
            (if-not (nil? x)
              (do
                (put-all! cs x)
                (recur))
              :done))))
    cs))

(defn copy-chan
  ([c]
    (first (multiplex c 1)))
  ([out c]
    (first (multiplex c [out]))))

(defn event-chan
  ([type] (event-chan js/window type))
  ([el type] (event-chan (chan) el type))
  ([c el type]
    (let [writer #(put! c %)]
      (.addEventListener el type writer)
      {:chan c
       :unsubscribe #(.removeEventListener el type writer)})))

(defn map-chan
  ([f source] (map-chan (chan) f source))
  ([c f source]
    (go-loop
      (>! c (f (<! source))))
    c))

(defn filter-chan
  ([f source] (filter-chan (chan) f source))
  ([c f source]
    (go-loop
      (let [v (<! source)]
        (when (f v)
          (>! c v))))
    c))

(defn jsonp-chan
  ([uri] (jsonp-chan (chan) uri))
  ([c uri]
    (let [jsonp (goog.net.Jsonp. (goog.Uri. uri))]
      (.send jsonp nil #(put! c %))
      c)))

(defn interval-chan
  ([msecs]
    (interval-chan msecs :leading))
  ([msecs type]
    (interval-chan (chan (dropping-buffer 1)) msecs type))
  ([c msecs type]
    (condp = type
      :leading (go-loop
                 (>! c (now))
                 (<! (timeout msecs)))
      :falling (go-loop
                 (<! (timeout msecs))
                 (>! c (now))))
    c))

;; using core.match could make this nicer probably - David

(defn throttle
  ([source msecs]
    (throttle (chan) source msecs))
  ([c source msecs]
    (go
      (loop [state ::init last nil cs [source]]
        (let [[_ sync] cs]
          (let [[v sc] (alts! cs)]
            (condp = sc
              source (condp = state
                       ::init (do (>! c v)
                                (recur ::throttling last
                                  (conj cs (timeout msecs))))
                       ::throttling (recur state v cs))
              sync (if last 
                     (do (>! c last)
                       (recur state nil
                         (conj (pop cs) (timeout msecs))))
                     (recur ::init last (pop cs))))))))
    c))

(defn debounce
  ([source msecs]
    (debounce (chan) source msecs))
  ([c source msecs]
    (go
      (loop [state ::init cs [source]]
        (let [[_ threshold] cs]
          (let [[v sc] (alts! cs)]
            (condp = sc
              source (condp = state
                       ::init
                         (do (>! c v)
                           (recur ::debouncing
                             (conj cs (timeout msecs))))
                       ::debouncing
                         (recur state
                           (conj (pop cs) (timeout msecs))))
              threshold (recur ::init (pop cs)))))))
    c))

(defn after-last
  ([source msecs]
    (after-last (chan) source msecs))
  ([c source msecs]
    (go
      (loop [cs [source]]
        (let [[_ toc] cs]
          (let [[v sc] (alts! cs :priority true)]
            (recur
              (condp = sc
                source (conj (if toc (pop cs) cs)
                         (timeout msecs))
                toc (do (>! c (now)) (pop cs))))))))
    c))

(defn fan-in
  ([ins] (fan-in (chan) ins))
  ([c ins]
    (go (while true
          (let [[x] (alts! ins)]
            (>! c x))))
    c))

(defn distinct-chan
  ([source] (distinct-chan (chan) source))
  ([c source]
    (go
      (loop [last ::init]
        (let [v (<! source)]
          (when-not (= last v)
            (>! c v))
          (recur v))))
    c))

(defprotocol IObservable
  (subscribe [c observer])
  (unsubscribe [c observer]))

(defn observable [c]
  (let [listeners (atom #{})]
    (go-loop
      (put-all! @listeners (<! c)))
    (reify
      proto/ReadPort
      (take! [_ fn1-handler]
        (proto/take! c fn1-handler))
      proto/WritePort
      (put! [_ val fn0-handler]
        (proto/put! c val fn0-handler))
      proto/Channel
      (close! [chan]
        (proto/close! c))
      IObservable
      (subscribe [this observer]
        (swap! listeners conj observer)
        observer)
      (unsubscribe [this observer]
        (swap! listeners disj observer)
        observer))))

(defn collection
  ([] (collection (chan) (chan (sliding-buffer 1)) {}))
  ([in events coll]
    (go
      (loop [coll coll cid 0 e nil]
        (when e
          (>! events e))
        (let [{:keys [op id val out]} (<! in)]
          (condp = op
            :query  (do (>! out (filter val (vals coll)))
                      (recur coll cid nil))
            :create (let [val (assoc val :id cid)]
                      (when out
                        (>! out val))
                      (recur (assoc coll cid val) (inc cid)
                        {:op :create :val val}))
            :read   (do (when out
                          (>! out (coll id)))
                      (recur coll cid
                        {:op :read :val (coll id)}))
            :update (recur (assoc coll id val) cid
                      {:op :update :prev (coll id) :val val})
            :delete (recur (dissoc coll id) cid
                      {:op :delete :val (coll id)})))))
    {:in in
     :events (observable events)}))

(defn view
  ([coll] (view (chan) coll))
  ([events coll] (view events coll identity))
  ([events coll f] (view events coll identity identity))
  ([events coll f sort]
    (let [events (subscribe (:events coll) (chan))]
      (go
        (>! (:in coll) {:op :query :val f})
        (loop [data (<! (:out coll))]
          (let [[[op val] sc] (alts! [events])]
            (condp = sc
              :create (recur (assoc data (:id val) val)))))))
    {:events (observable events)}))

;; hmm renderer could listen to debounced stream of events?

(defn renderer [view strategy]
  )

;; wishful thinking, or how to stop storing references and state
;; in the DOM

(declare view-chan extract)


(defn edit
  ([el id coll] (edit (chan) el id coll))
  ([control el id coll]
    (let [elc (view-chan el)]
      (go
        (loop []
          (let [[v sc] (alts! [elc control])]
            (condp = sc
              control (if (= v :exit)
                        :done
                        (recur))
              elc (if (= v :change)
                    (>! (:in coll) {:op :update :id id :val (extract el)})
                    (recur))))))
      control)))
