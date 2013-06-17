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


(defn add-defonce
  [global sym-name value]
  (-> global
    (aget  "defonce-instances")
    (aset (str sym-name) value)))


(defn get-defonce
  [global sym-name]
  (-> global
    (aget "defonce-instances")
    (aget (str sym-name))))


(defmacro defonce
  [sym-name body]
  `(if-let [stored-value# (get-defonce js/global ~sym-name)]
      (add-defonce js/global
        ~sym-name
        (def ~sym-name stored-value#))
      (def ~sym-name ~body)))

