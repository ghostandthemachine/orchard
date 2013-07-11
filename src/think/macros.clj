(ns think.macros
  [:refer-clojure :exclude [defonce]]
  (:require [clojure.string :as str]))


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




(defmacro defone
  "A symbol based version of defonce which creates global, non ns qualified, vars."
  [sym-name value]
    `(def ~sym-name
      (or
        (aget js/global (name '~sym-name))
        (aset js/global (name '~sym-name) ~value))))


(defn nskw->sym
  [nskw]
  (symbol (last (str/split (apply str (rest (str nskw))) #"/"))))


(defmacro defonce
  [nskw value]
  `(def ~(nskw->sym nskw)
    (or
      (aget js/global ~nskw)
      (aset js/global ~nskw ~value))))


(defmacro mac
  [nskw value]
  `(.log js/console "nskw = " ~nskw))





(comment


(def data (atom {:foo "bar" :bar "baz"}))

(defmacro defoo
  [sym-name value]
    `(def ~sym-name
      (or
        (get @data (name '~sym-name))
        (get (swap! data assoc (name '~sym-name) ~value)
          (name '~sym-name)))))

(def data (atom {:foo "bar" :bar "baz"}))

(defn nskw->sym
  [nskw]
  (symbol (last (str/split (apply str (rest (str nskw))) #"/"))))

(defmacro defoo
  [nskw value]
  (println )
  `(def ~(nskw->sym nskw)
    (or
      (get @data '~nskw)
      (get (swap! data assoc '~nskw ~value)
        (name '~nskw)))))


)