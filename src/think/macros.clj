(ns think.macros
  [:refer-clojure :exclude [defonce]])


(defmacro with-instance
  [db-name & body]
  `(pouch ~db-name (cljs.core/clj->js {})
          (fn [err# db#]
            (-> db# ~@body))))


(defmacro defui
  [sym params hiccup & events]
  `(defn ~sym ~params
     (let [e# (crate.core/html ~hiccup)]
       (doseq [[ev# func#] (partition 2 ~(vec events))]
         (think.util.dom/on-event e# ev# func#))
       e#)))


(defmacro defgui
  [sym params hiccup & events]
  `(defn ~sym ~params
     (let [e# (crate.core/html ~hiccup)]
       (doseq [[ev# func#] (partition 2 ~(vec events))]
         (think.util.dom/on-event e# ev# (partial func# e#)))
       e#)))




(defmacro defonce
  [sym-name value]
    `(def ~sym-name
      (or
        (aget js/global (name '~sym-name))
        (aset js/global (name '~sym-name) ~value))))








(comment


(def data (atom {:foo "bar" :bar "baz"}))

(defmacro defoo
  [sym-name value]
    `(def ~sym-name
      (or
        (get @data (name '~sym-name))
        (get (swap! data assoc (name '~sym-name) ~value)
          (name '~sym-name)))))


)