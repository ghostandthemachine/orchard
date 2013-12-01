(ns orchard.sidebar
  (:require-macros
            [orchard.macros :refer (defui defgui)])
  (:require [crate.core :as crate]
            [orchard.util.dom :as dom]
            [orchard.util.log :refer (log log-obj)]))


(defui sidebar-item
  [s handler]
  [:a {:href "#"} s]
  :click handler)


(defn toggle-menu
  [btn menu]
  ;; toggle button
  (dom/toggle-class btn "active")
  (dom/toggle-class btn "slide-menu-btn-open")
  ;; toggle menu
  (dom/toggle-class menu "active")
  (dom/toggle-class menu "slide-menu-open")
  (dom/focus menu))


(defn hide-menu
  [menu]
  (dom/remove-class menu "active")
  (dom/add-class menu "slide-menu-close"))


(defgui show-btn
  [menu]
  [:span.slide-menu-btn.slide-menu-btn-vertical.slide-menu-btn-right
    [:i.glyphicon.white.glyphicon-list]]
  :click (fn [btn e] (toggle-menu btn menu)))


(defui menu
  [elems item-view]
  [:nav.slide-menu.slide-menu-vertical.slide-menu-right
    (for [[title handler] elems]
      (item-view title handler))])


(defn sidebar-components
  []
  (let [menu  (menu
                [["Projects" (fn [] (log "clicked projects"))]
                 ["Foo" (fn [] (log "clicked foo"))]
                 ["Bar" (fn [] (log "clicked bar"))]]
                 sidebar-item)
        sbtn  (show-btn menu)]
    [menu sbtn]))
