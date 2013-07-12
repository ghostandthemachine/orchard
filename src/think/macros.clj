(ns think.macros
  [:refer-clojure :exclude [defonce]]
  (:require [clojure.string :as str]
            [cljs.analyzer :refer (*cljs-ns* get-expander)]))


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


(defmacro sym->nssym
  [sym]
  `(str '~*cljs-ns* "/" ~(name sym)))

(defmacro defonce
  ^{:doc
    "Clojurescript version of defonce. Uses ns qaulified keyword as key to store val in global browser object."}
  [n value]
  `(def ~n
    (or
      (aget js/global (sym->nssym ~n))
      (aset js/global (sym->nssym ~n) ~value))))
