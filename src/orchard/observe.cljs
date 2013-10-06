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
  [node handler & config]
  (log "observing node")
  (log-obj node)

  (let [obs (observer handler)
        opts (clj->js (reduce #(assoc %1 (js-style-name (name %2)) true) {} config))]
    (log-obj opts)
    (.observe obs node opts)
    obs))


(defn handle-node-ready
  "Given a node to query for, a channel, and an Array of MutationRecords,
  the querried node will be pushed onto the channel once found."
  [node chan records]
  (log-obj records)
  (let [n (filter #(= node %)
            (apply concat (map (fn [mr] (aget mr "addedNodes")) records)))]
    (log "matched added node")
    (log-obj n)
    (go
      (when n
        (>! chan node)))))


(defn dom-ready-chan
  "Takes a Thinker Object (atom) and attaches an on-ready observer if a :ready handler is registered in the object."
  [elem]
  (let [ready-chan (chan)
        ; observer (observe js/document.body (partial handle-node-ready elem ready-chan) :subtree)
        ]
    (go
      (let [created (<! ready-chan)]
        (.disconnect observer)
        elem))))