(ns orchard.observe
  (:refer-clojure :exclude [find])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [orchard.util.dom :refer (append $ by-id find)]
            [orchard.util.core :refer [js-style-name]]
            [cljs.core.async :refer [chan >! <! put! timeout close!]]
            [orchard.util.log :refer (log log-obj)]))


(defn observer [handler]
  (new js/WebKitMutationObserver handler))


(defn observe
  [elem handler & config]
  (let [obs (observer handler)
        opts (clj->js (reduce #(assoc %1 (js-style-name (name %2)) true) {} config))]
    (.observe obs elem opts)
    obs))


(defn observer-chan
  [elem & config]
  (let [c (chan)]
    (new js/WebKitMutationObserver (partial put! c)
      (reduce #(assoc %1 (js-style-name (name %2)) true) {} config))
    c))


(defn handle-node-ready
  "Given a node to query for, a channel, and an Array of MutationRecords,
  the querried node will be pushed onto the channel once found."
  [el chan records]
  (let [nodes (apply concat (map (fn [mr] (aget mr "addedNodes")) records))
        n     (filter #(= el %) nodes)]
    (go
      (when n
        (>! chan n)))))


(defn ready-observer
  "Takes a Thinker Object (atom) and attaches an on-ready observer if a :ready handler is registered in the object."
  [obj]
  (log "ready-observer")
  (let [el          (:content obj)
        ready-chan  (chan)
        observer    (observe js/document.body
                      (partial handle-node-ready el ready-chan)
                      :child-list :subtree)]
    (go
      (let [created (<! ready-chan)]
        (.disconnect observer)
        ((:ready obj) obj)))))


(defn dom-ready-chan
  "Takes an element and attaches an on-ready observer returning the element when it is added to the dom"
  [elem]
  (let [ready-chan (chan)
        ;; NOTE: this is a problem line. When :child-list is not present the app crashes
        ;; probably an issue in the fn observe
        observer (observe js/document.body (partial handle-node-ready elem ready-chan) :child-list :subtree)]
    (go
      (let [created (<! ready-chan)]
        (.disconnect observer)
        elem))))