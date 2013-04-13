(ns think.macros)


(defmacro with-instance
  [db-name & body]
  `(pouch ~db-name (cljs.core/clj->js {})
          (fn [err# db#]
            (-> db# ~@body))))


(defmacro defview [sym params hiccup & events]
  `(defn ~sym ~params
     (let [e# (dommy.template/node ~hiccup)]
       (doseq [[ev# func#] (partition 2 ~(vec events))]
         (dommy.core/listen! e# ev# func#))
       e#)))
