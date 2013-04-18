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
