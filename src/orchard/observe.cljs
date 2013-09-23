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
  (let [obs (observer handler)]
    (.observe obs node (clj->js (reduce #(assoc %1 (js-style-name (name %2)) true) {} config)))
    obs))


(defn handle-node-ready
  "Given a node to query for, a channel, and an Array of MutationRecords,
  the querried node will be pushed onto the channel once found."
  [node chan records]
  (let [node-lists (filter #(> (count %) 0)
                    (map (fn [mr] (aget mr "addedNodes")) records))]
    (doseq [nl node-lists]
      (doseq [parent (distinct nl)]
        (when-let [child (find node parent)]
          (go
            (>! chan child)))))))


(defn add-ready-observer
  "Takes a Thinker Object (atom) and attaches an on-ready observer if a :ready handler is registered in the object."
  [obj]
  (when (:ready @obj)
    (let [node (:content @obj)
          ready-chan (chan)
          observer (observe js/document.body (partial handle-node-ready node ready-chan) :child-list :subtree)]
      (go
        (let [created (<! ready-chan)]
          (.disconnect observer)
          ((:ready @obj) obj))))))
