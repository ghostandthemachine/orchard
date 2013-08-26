(ns orchard.util.time)

;; Timers

(defn periodically
  "Call f on a given interval until f returns :stop."
  [interval f]
  (let [inter   (atom nil)
        handler (fn []
                  (if (= :stop (f))
                    (js/clearInterval @inter)))]
    (reset! inter (js/setInterval handler interval))
    @inter))


(defn clear-interval
  "Cancel the periodic invokation specified by the given interval id."
  [interval-id]
  (js/clearInterval interval-id))

(defn run-in
  "Run a function after a ms milleseconds."
  [ms func]
  (js/setTimeout func ms))

(defn now
  "Get the current time/date."
  []
  (.getTime (js/Date.)))

;(defn toggler [cur op op2]
;  (if (= cur op)
;    op2
;    op))

; (defn debounce [ts func]
;   (.debounce js/$ ts func))

; (defn throttle [ts func]
;   (.throttle js/$ ts func))

;(defn ->clj [data]
;  (js->clj data :keywordize-keys true))
