(ns think.macros
  [:refer-clojure :exclude [defonce]])


(defmacro with-instance
  [db-name & body]
  `(pouch ~db-name (cljs.core/clj->js {})
          (fn [err# db#]
            (-> db# ~@body))))


(defmacro defui [sym params hiccup & events]
  `(defn ~sym ~params
     (let [e# (crate.core/html ~hiccup)]
       (doseq [[ev# func#] (partition 2 ~(vec events))]
         (think.util.dom/on-event e# ev# func#))
       e#)))


(defmacro defgui [sym params hiccup & events]
  `(defn ~sym ~params
     (let [e# (crate.core/html ~hiccup)]
       (doseq [[ev# func#] (partition 2 ~(vec events))]
         (think.util.dom/on-event e# ev# (partial func# e#)))
       e#)))


(defmacro defonce
  [sym-name value]
  ; `(let [defonce-instances# (aget js/global "defonce-instances")]
      `(.log js/console "defonce value exists for symbol: " ~(name sym-name))
      `(.log js/console "instances " (.-defonce-instances js/global))
    ; `(if-let [stored-val# (get defonce-instances# ~(name sym-name))]
        ; (def ~sym-name stored-val#)
        ; `(.log js/console "defonce value exists for symbol: " ~(name sym-name))
        ; `(.log js/console "defonce value does not exists for symbol: " ~(name sym-name))
        ; (aset js/global
        ;   ~(name sym-name)
        ;   (def ~sym-name ~value))
        ; )
    ; )
)


