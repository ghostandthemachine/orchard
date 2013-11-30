(ns orchard.util.log
  (:require [orchard.dispatch :as dispatch]))


(defn log
  "Print a log message to the console."
  [v & text]
  (let [vs (cond 
             (string? v) (apply str v text)
             (= cljs.core/PersistentArrayMap (type v)) (clj->js v)
             (= cljs.core/PersistentVector   (type v)) (clj->js v)
             :default (str v))]
    (dispatch/fire :log-message vs)
    (.log js/console vs)))


; TODO: add some kind of automatic clj->js and pretty-print support for more easily
; outputting objects, types, nested object structures, etc...
(defn log-obj
  "Print a JS object to the console."
  [v]
  (let [obj (cond 
             (= cljs.core/PersistentArrayMap (type v)) (clj->js v)
             (= cljs.core/PersistentVector   (type v)) (clj->js v)
             :default v)]
    (.log js/console obj)
    v))


(defn timeline
  "Create a timeline entry if in timeline record mode."
  [msg]
  (.timeStamp js/console (str msg)))


(defn profile-start
  [name]
  (.time js/console (str name)))


(defn profile-end
  [name]
  (.timeEnd js/console (str name)))


;(defn trace-ns
;  "ns should be a namespace object or a symbol."
;  [ns]
;  (doseq [s (keys (ns-interns ns))
;          :let [v (ns-resolve ns s)]
;          :when (and (ifn? @v) (-> v meta :macro not))]
;    (intern ns
;            (with-meta s {:traced true :untraced @v})
;            (let [f @v] (fn [& args]
;                          (clojure.contrib.trace/trace (str "entering: " s))
;                          (apply f args))))))
;
;(defn untrace-ns [ns]
;  (doseq [s (keys (ns-interns ns))
;          :let [v (ns-resolve ns s)]
;          :when (:traced (meta v))]
;    (alter-meta! (intern ns s (:untraced (meta v)))
;                 #(dissoc % :traced :untraced))))
;


(defn jslog [& m]
  (log (apply js->clj m)))


(defn log-err
  [v & [text]]
  (log v text))


;; logging with flags


(def log-flags* (atom #{:global}))


(defn in?
  ([x]
    (in? @log-flags* x))
  ([coll x]
  (if (some #{x} @log-flags*)
    true
    false)))


(defn set-flags
  [flags]
  (reset! log-flags* (into #{} flags)))


(defn add-flags
  [& flags]
  (swap! log-flags* #(into #{} (concat % flags))))


(defn remove-flags
  [flags]
  (swap! log-flags*
    (fn [fs]
      (remove #(in? flags %) fs))))


(defn flagged?
  [flag]
  (not (nil?
    (some #{flag} @log-flags*))))


(defn log$
  ; ([arg & args]
  ;   (log$ :global arg args))
  ([flags & args]
    (let [flags (flatten [flags])
          flagged? (reduce #(or %1 (in? %2)) false flags)
          args (flatten args)]
      (println flags flagged? args)
      (when flagged?
        (log "LOG: flags = " flags)
        (log args)))))

