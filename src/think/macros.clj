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
  (symbol
    (last
      (str/split
        (apply str (rest (str nskw)))
        #"/"))))


(defmacro defonce
  {^:doc "A Clojurescript version of defonce which takes a namespace qualified keyword
  and creates a global, namespaced var. This will will result in a normal var
  definition.

  ex:
  (defonce ::foo \"bar\")

  will result in a var named foo in the namespace where it was defined. So from ns my-ns,

  (println foo)
  => bar

  (println my-ns/foo)
  => bar"}
  [nskw value]
  `(def ~(nskw->sym nskw)
    (or
      (aget js/global ~nskw)
      (aset js/global ~nskw ~value))))