(ns orchard.editable.toolbar
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [orchard.macros :refer [defui]])
  (:require [orchard.util.dom :as dom]
            [orchard.editable.core :as ed]
            [orchard.model :as model]
            [crate.core :as crate]
            [clojure.string :as string]
            [cljs.core.async :refer (chan >! <! timeout put! get!)]))


(defn icon [n]
  [:span {:class (str "glyphicon " n)}])

;; text attr group
(defui bold-btn []
  [:button {:class "btn btn-default bold-btn" :type "button"} "B"]
  :click (fn [_] (ed/apply-attribute :bold)))

(defui italic-btn []
  [:button {:class "btn btn-default italic-btn" :type "button"} [:i "I"]]
  :click (fn [_] (ed/apply-attribute :italic)))

(defui underline-btn []
  [:button {:class "btn btn-default underline-btn" :type "button"} "U"]
  :click (fn [_] (ed/underline-selection)))

(defui strike-through-btn []
  [:button {:class "btn btn-default strike-through-btn" :type "button"} [:strike "S"]]
  :click (fn [_] (ed/strike-through-selection)))



(defui anchor-btn []
  [:button {:class "btn btn-default anchor-btn" :type "button"}
    (icon (str "glyphicon-link"))]
  :click (fn [_]
            (when (not= (ed/selected-text) "")
              (go
                (let [db          orchard.objects.app/db
                      s           (string/trim (ed/selected-text))
                     [proj-title page-title]
                                  (string/split s #"/")
                      proj-id     (:id (first (<! (model/project-by-title db proj-title))))
                      page-id     (:id (first (<! (model/page-by-title db proj-id page-title))))]
                  (ed/create-link page-id))))))


;; format block group
(defui blockquote-btn []
  [:button {:class "btn btn-default" :type "button"} "\""]
  :click (fn [_] (ed/format-block :blockquote)))

(defui header-btn [size]
  [:button {:class "btn btn-default" :type "button"} (str "H" size)]
  :click (fn [_] (ed/format-block (str "H" size))))

;; List group
(defui ordered-list-btn []
  [:button {:class "btn btn-default justify-btn" :type "button"}
    (icon (str "glyphicon-list"))]
  :click (fn [_] (ed/insert-element :ol)))

(defui unordered-list-btn []
  [:button {:class "btn btn-default justify-btn" :type "button"}
    (icon (str "glyphicon-list"))]
  :click (fn [_] (ed/insert-element :ul)))

;; justify group
(defui justify-btn [loc]
  [:button {:class "btn btn-default justify-btn" :type "button"}
  (icon (str "glyphicon-align-" (name loc)))]
  :click (fn [_] (ed/justify-selection loc)))


(defn view
  [& opts]
  (let [opts (apply hash-map (flatten (partition 2 opts)))]
    [:div.editable-toolbar
      [:div {:class "btn-group btn-group-sm editable-toolbar-subgroup"}
        (bold-btn)
        (italic-btn)
        (underline-btn)
        (strike-through-btn)
        (anchor-btn)]
      [:div  {:class "btn-group btn-group-sm editable-toolbar-subgroup"}
        (blockquote-btn)
        (header-btn 1)
        (header-btn 2)
        (header-btn 3)
        (header-btn 4)]
      [:div  {:class "btn-group btn-group-sm editable-toolbar-subgroup"}
        (ordered-list-btn)
        (unordered-list-btn)]
      [:div  {:class "btn-group btn-group-sm editable-toolbar-subgroup"}
        (justify-btn :left)
        (justify-btn :center)
        (justify-btn :right)]]))