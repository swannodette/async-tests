(ns async-test.utils.helpers
  (:require
    [cljs.core.async :as async
     :refer [<! >! chan close! sliding-buffer dropping-buffer
             put! timeout]]
    [goog.net.Jsonp]
    [goog.Uri])
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

(defn by-id [id] (.getElementById js/document id))

(defn set-html [el s]
  (aset el "innerHTML" s))

(defn to-char [code]
  (.fromCharCode js/String code))

(defn set-class [el name]
  (set! (.-className el) name))

(defn clear-class [el name]
  (set! (.-className el) ""))

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
  ([el type] (event-chan (chan (sliding-buffer 1)) el type))
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

(defn throttle
  ([source msecs]
    (throttle (chan) source msecs))
  ([c source msecs]
    (throttle c source msecs nil))
  ([c source msecs reset]
    (go
      (loop [cs [(or reset (chan)) source]]
        (let [[_ _ sync] cs]
          (when sync (<! sync))
          (let [[v sc] (alts! cs :priority true)]
            (recur
              (condp = sc
                reset (do (<! v)
                        (if sync (pop cs) cs))
                source (do (>! c v)
                         (if sync
                           cs
                           (conj cs (interval-chan msecs :falling))))
                sync (pop cs)))))))
    c))

(defn debounce
  ([source msecs] (debounce (chan) source msecs))
  ([c source msecs]
    (go
      (loop [cs [source]]
        (let [toc (second cs)]
          (when-not toc (>! c (<! source)))
          (let [[v sc] (alts! cs)]
            (recur
              (if (= sc source)
                (conj (if-not toc cs (pop cs)) (timeout msecs))
                (pop cs)))))))
    c))

(defn after-last
  ([source msecs]
    (after-last (chan) source msecs))
  ([c source msecs]
    (after-last (chan) source msecs nil))
  ([c source msecs reset]
    (let [skip (chan)]
      (go
        (loop [cs [(or reset (chan)) source]]
          (let [[_ _ toc] cs]
            (let [[v sc] (alts! cs :priority true)]
              (recur
                (condp = sc
                  reset (do (<! v)
                          (if toc (pop cs) cs))
                  source (conj (if toc (pop cs) cs)
                           (timeout msecs))
                  toc (do (>! c (now)) (pop cs))))))))
      c)))

(defn fan-in [ins]
  (let [c (chan)]
    (go (while true
          (let [[x] (alts! ins)]
            (>! c x))))
    c))

(defn uniq-chan
  [c]
  (let [c' (chan)]
    (go
     (loop [v ::_initial_ v' (<! c)]
       (when (not= v v')
         (>! c' v'))
       (recur v' (<! c))))
    c'))

(defn timeout-chan [ms source]
  (let [c' (chan)]
    (go
     (let [t (timeout 500)
           [cc channel] (alts! [source t])]
       (if (= t channel)
         (close! c')
         (>! c' cc))))
    c'))

(defn throt [ms c]
  (let [c' (chan 1)]
    (go
     (loop [v1 ::_none_
            t (timeout ms)]
       (let [[v ch]
             (alts! [c t])]
         (if (= c ch)
           (recur v t)
           (do
             (if (= ::_none_ v1)
               (>! c' (<! c))
               (>! c' v1))
             (recur ::_none_ (timeout ms)))))))
    c'))

(defn monitor [name source]
  (let [c' (chan)]
    (go
     (loop [v (<! source)]
       (.log js/console (str name ": " v))
       (>! c' v)
       (recur (<! source))))
    c'))

(defn trans-chan [f source]
  (let [c' (chan)]
    (go-loop
     (>! c' (<! (f (<! source)))))
    c'))
