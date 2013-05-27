(ns think.util.log)

(defn log
  "Print a log message to the console."
  [v & text]
  (let [vs (if (string? v)
             (apply str v text)
             v)]
    (.log js/console vs)))


; TODO: add some kind of automatic clj->js and pretty-print support for more easily
; outputting objects, types, nested object structures, etc...
(defn log-obj
  "Print a JS object to the console."
  [obj]
  (.log js/console obj)
  obj)

; (defn log [& m]
;   (.log js/console (apply str m)))

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
  ([arg & args]
    (log$ :global arg args))
  ([flags & args]
    (let [flags (flatten [flags])
          flagged? (reduce #(or %1 (in? %2)) false flags)
          args (flatten args)]
      (println flags flagged? args)
      (when flagged?
        (log "LOG: flags = " flags)
        (log args)))))



