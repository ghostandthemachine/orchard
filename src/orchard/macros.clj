(ns orchard.macros
  [:refer-clojure :exclude [defonce]]
  (:require [clojure.string :as str]
            ;[cljs.core.async :refer [go chan >! <! put! timeout alts!]]
            [cljs.analyzer :refer (*cljs-ns* get-expander)]
            ))


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
         (orchard.util.dom/on-event e# ev# func#))
       e#)))


(defmacro defgui
  [sym params hiccup & events]
  `(defn ~sym ~params
     (let [e# (crate.core/html ~hiccup)]
       (doseq [[ev# func#] (partition 2 ~(vec events))]
         (orchard.util.dom/on-event e# ev# (partial func# e#)))
       e#)))


(defmacro nssym
  [sym]
  `(str '~*cljs-ns* "/" ~(name sym)))

(defmacro defonce
  ^{:doc
    "Clojurescript version of defonce.
    Stores variables in the javascript global function. They will persist through browser refreshes."}
  [n value]
 `(def ~n
    (or
      (aget js/global (nssym ~n))
      (aset js/global (nssym ~n) ~value))))


(defmacro node-chan
  "Appends a callback to a given form which takes two arguments `[error value]`
and executes it, returning a channel that will receive `error` if `error`
is truthy, or `value` if `error` is falsy. This is a standard
node.js callback scheme. For example:

    (node-chan (.readFile fs \"/etc/passwd\"))
"
  ([form]
     `(node-chan ~form identity))
  ([form transformer]
     `(let [chan# (cljs.core.async/chan)
            callback# (fn [error# value#]
                        (if error#
                          (cljs.core.async/put! chan# {:error error# :value nil})
                          (cljs.core.async/put! chan# {:error nil :value (~transformer value#)})))]
        (~@form callback#)
        chan#)))
