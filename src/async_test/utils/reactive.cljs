(ns async-test.utils.reactive
  (:refer-clojure :exclude [map filter remove distinct])
  (:require
    [cljs.core.async :as async
     :refer [<! >! chan close! sliding-buffer dropping-buffer
             put! timeout]]
    [cljs.core.async.impl.protocols :as proto]
    [async-test.utils.helpers :as h]
    [goog.net.Jsonp]
    [goog.Uri]
    [goog.dom :as dom])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt!]]
    [async-test.utils.macros :refer [go-loop]]))

(defn now []
  (.valueOf (js/Date.)))

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

(defn copy
  ([c]
    (first (multiplex c 1)))
  ([out c]
    (first (multiplex c [out]))))

(defn events
  ([type] (events js/window type))
  ([el type] (events (chan) el type))
  ([c el type]
    (let [writer #(put! c %)]
      (.addEventListener el type writer)
      {:chan c
       :unsubscribe #(.removeEventListener el type writer)})))

(defn map
  ([f source] (map (chan) f source))
  ([c f source]
    (go-loop
      (>! c (f (<! source))))
    c))

(defn filter
  ([f source] (filter (chan) f source))
  ([c f source]
    (go-loop
      (let [v (<! source)]
        (when (f v)
          (>! c v))))
    c))

(defn remove
  ([f source] (remove (chan) f source))
  ([c f source]
    (go-loop
      (let [v (<! source)]
        (when-not (f v)
          (>! c v))))
    c))

(defn jsonp
  ([uri] (jsonp (chan) uri))
  ([c uri]
    (let [gjsonp (goog.net.Jsonp. (goog.Uri. uri))]
      (.send gjsonp nil #(put! c %))
      c)))

(defn interval
  ([msecs]
    (interval msecs :leading))
  ([msecs type]
    (interval (chan (dropping-buffer 1)) msecs type))
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

(defn distinct
  ([source] (distinct (chan) source))
  ([c source]
    (go
      (loop [last ::init]
        (let [v (<! source)]
          (when-not (= last v)
            (>! c v))
          (recur v))))
    c))

(defn hover-chan [el tag]
  (let [matcher (h/tag-match tag)
        matches (h/by-tag-name el tag)
        raw-over (events el "mouseover")
        raw-out (events el "mouseout")
        over (->> (:chan raw-over)
               (map
                 #(let [target (.-target %)]
                    (if (matcher target)
                      target
                      (if-let [el (dom/getAncestor target matcher)]
                        el
                        :no-match))))
               (map
                 #(h/index-of matches %)))
        out (->> (:chan raw-out)
              (filter
                (fn [e]
                  (and (matcher (.-target e))
                       (not (matcher (.-relatedTarget e))))))
              (map #(do :out)))]
    {:chan (distinct (fan-in [over out]))
     :unsubscribe
      #(do
         ((:unsubscribe raw-over))
         ((:unsubscribe raw-out)))}))

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
