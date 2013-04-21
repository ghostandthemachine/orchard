(ns think.graph-model
  (:refer-clojure :exclude [create-node])
  (:use [think.util.log :only (log log-obj)])
  (:require [think.util :as util]
            [think.dispatch :as dispatch]))

;=================================================================
; nodes and edges
;=================================================================

(def ^{:private true} nodes (atom {}))
(def ^{:private true} edges (atom {}))

(def default-edge-data {:length 0.8
                        :alpha  1.0})

(def default-node-data {:color "blue"
                        :alpha  1.0
                        :body "FIXME."
                        :mass   100
                        :fixed false
                        :renderer :circle
                        :text-attributes {:color "white"
                                          :align "center"
                                          :font "Arial"
                                          :size 12}
                        :modules []})

(defn get-node [node-id] (node-id @nodes))

(defn get-edge [edge-id] (edge-id @edges))
(defn get-edges [node-id] (get-in @nodes [node-id :edges]))

(defn create-edge
  [src target data]
  (let [id (util/uuid)]
    (swap! edges assoc id data)
    (swap! nodes (fn [nds] (update-in nds [src :edges] #(conj % id))))
    (swap! nodes (fn [nds] (update-in nds [target :edges] #(conj % id))))
    (dispatch/fire :edge-created {:src src :target target :id id :data (id @edges)})
    id))

(defn create-node
  [data]
  (let [node-id (util/uuid)]
    (swap! nodes assoc node-id (merge default-node-data data))
    (when-let [new-data (node-id @nodes)]
      ;; remove the parent as it is stored in an edge
      (dispatch/fire :node-created {:id node-id :data (dissoc new-data :parent)}))
    (if-let [parent (:parent data)]
      (create-edge node-id parent default-edge-data))
    node-id))

(defn update-node
  [id data]
  (when-let [updated-data (swap! nodes assoc id data)]
    (dispatch/fire :node-updated {:id id})
    updated-data))

(defn update-node-modules
  [id data]
  (when-let [updated-data (swap! nodes assoc-in [id :modules] data)]
    (dispatch/fire :node-updated {:id id})
    updated-data))

(defn update-edge
  [id data]
  (when-let [updated-data (swap! edges assoc id data)]
    (dispatch/fire :edge-updated {:id id})
    updated-data))

(defn get-node-name [id] (get-in @nodes [id :name]))
(defn get-node-body [id] (get-in @nodes [id :body]))

;; called directly from view now
; (dispatch/react-to #{:create-node} #(create-node %2))

;=================================================================
; modules
;=================================================================

(def ^{:private true} modules (atom {}))

(defn get-module [module-id] (module-id @modules))

(defn get-modules [node-id] (get-in @nodes [node-id :modules]))

(defn add-module-to-node [node-id module-id]
  (swap! nodes
    (fn [node-map]
      (assoc-in node-map [node-id :modules] (conj (get-in [node-id :modules] node-map) module-id)))))

(defn remove-module-from-node [module-id]
  (let [node-id (:parent (get-module module-id))]
    (swap! nodes
      (fn [node-map]
        (assoc-in node-map [node-id :modules] (filter #(= module-id %) node-map))))
    (dispatch/fire :module-removed module-id)
    (get-in @nodes [node-id :modules])))


(defn create-module [node-id data]
  (let [module-id (util/uuid)]
    (swap! modules assoc module-id (merge data {:parent node-id :module-id module-id}))
    (when-let [new-data (module-id @modules)]
      (dispatch/fire :module-created {:id module-id :data new-data}))
    module-id))

(defn update-module [module-id data]
  (let [new-data (module-id (swap! modules assoc-in [module-id :data] data))]
    (dispatch/fire :module-updated module-id)))

(defn destroy-module [module-id]
  (let [node-id (:parent (get-module module-id))]
    (remove-module-from-node module-id)
    (swap! modules dissoc module-id)
    (dispatch/fire :module-destroyed node-id)))

(dispatch/react-to #{:create-module} #(create-module %2 %3))






(def ^{:private true} graph-state (atom {}))

(defn set-graph-attributes [data]
  (reset! graph-state data)
  (dispatch/fire :graph-view-attributes-updated data))

(defn graph-attributes [] @graph-state)
