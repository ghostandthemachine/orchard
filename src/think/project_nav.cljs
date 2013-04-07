(ns think.project-nav
  (:use-macros [dommy.macros :only [sel]])
  (:use [think.util :only [log jslog uuid]])
  (:require [think.dispatch :as dispatch]
            [think.graph-model :as model]
            [dommy.core :as dom]
            [dommy.template :as dt]))


(defn create-tree-container
  [id]
  (dt/node
    [:div.tree-container {:id (str id "-container")}]))

(defn create-tree-element
  [tree-id text & [opts]]
  [:div.row.tree-row
    [:span.tree-element-text text]])

(defn add-item
  [tree text handler & [opts]]
  (let [child-element (dt/node (create-tree-element tree text opts))]
    (dom/append! tree child-element)
    (dom/listen! child-element :click handler)
    child-element))

(comment

(def tree (create-tree-container "project-tree"))

(def c (add-item tree "Testing" #(log "Testing handler")))





  )