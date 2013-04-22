(ns think.util.js)

(defn every [ms func]
  (js/setInterval func ms))

(defn wait [ms func]
  (js/setTimeout func ms))

(defn now []
  (.getTime (js/Date.)))

(defn toggler [cur op op2]
  (if (= cur op)
    op2
    op))

; (defn debounce [ts func]
;   (.debounce js/$ ts func))

; (defn throttle [ts func]
;   (.throttle js/$ ts func))

(defn ->clj [data]
  (js->clj data :keywordize-keys true))
