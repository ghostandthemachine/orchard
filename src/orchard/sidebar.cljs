(ns orchard.sidebar
  (:require-macros
            [cljs.core.async.macros :refer [go]]
            [orchard.macros :refer (defui defgui)])
  (:require [crate.core :as crate]
            [orchard.util.dom :as dom]
            [orchard.model :as model]
            [orchard.util.log :refer (log log-obj)]
            [cljs.core.async :refer [chan >! <! put!]]))


(defn child-selector
  [id]
  (str "#child-" id))


(defn icon [n]
  [:span {:class (str "glyphicon " n " sidebar-icon")}])


(defui sidebar-item
  [title id]
  [:span.btn.tree-item.sidebar-element {:href "#"}
    [:span.tree-child-gutter]
    (icon (str "glyphicon-file"))
    title]
  :click  (fn [e]
            (orchard.objects.app/open-page orchard.objects.app/db id)))


(defui sidebar-project-item
  [title id]
  [:span.btn.tree-item.sidebar-element {:href "#"}
    (icon (str "glyphicon-folder-open"))
    title]
  :click  (fn [e]
            (orchard.objects.app/open-page orchard.objects.app/db id)))


; (defgui tree-toggler
;   [title id]
;   [:a {:href "#"} title]
;   :click  (fn [el e]
;             (let [parent  (.parent (dom/$ el))
;                   cs      (.children parent "ul.tree")]
;               (log "click toggler")
;               (log-obj el)
;               (log-obj parent)
;               (log-obj cs)
;               (.toggle cs 300))))


(defn project-element
  [proj]
  [:li.tree-parent
    [:span.tree-toggler.nav-header
      (sidebar-project-item (:title proj) (:root proj))]
    [:ul.nav.nav-list.tree
      (for [d (filter #(not (nil? (:title %))) (:documents proj))]
        [:li.tree-child
          (sidebar-item (:title d) (:id d))])]])



(defn toggle-menu
  []
  (let [menu (dom/$ "#project-sidebar")]
    ;; toggle menu
    (dom/toggle-class menu "slide-menu-open")
    (dom/toggle-class menu "active")
    (dom/focus menu)))


(defn toggle-btn
  []
  (let [btn  (dom/$ "#sidebar-toggle")]
    ;; toggle button
    (dom/toggle-class btn "slide-menu-btn-open")
    (dom/toggle-class btn "active")))


(defn open?
  []
  (dom/has-class? (dom/$ "#project-sidebar") "active"))


(defn hide-menu
  [menu]
  (dom/remove-class menu "active")
  (dom/add-class menu "slide-menu-close"))



(defn make-menu
  [projects]
  (let [c (str "slide-menu slide-menu-vertical slide-menu-right"
            (when (open?) " slide-menu-open active"))
        hic [:nav {:id "project-sidebar"
                   :class c}
              [:ul.nav.nav-list
                (for [proj projects]
                  (project-element proj))]]
        el  (crate/html hic)]
    el))

(defn update-sidebar
  []
  (go
    (let [db        orchard.objects.app/db
          ps        (into []
                      (filter
                        (fn [p]
                          (not= (:id p) "home"))
                        (<! (model/all-projects db))))
          projects  (loop [res [] projs ps]
                      (if (empty? projs)
                        res
                        (let [id (:id (first projs))
                              p (<! (model/get-object-with-dependants db id :documents))
                              res (conj res p)]
                          (recur res (rest projs)))))
          old-sidebar   (dom/$ "#project-sidebar")
          new-sidebar   (make-menu projects)]
    (dom/replace-with old-sidebar new-sidebar))))


(defgui show-btn
  [menu]
  [:span#sidebar-toggle.slide-menu-btn.slide-menu-btn-vertical.slide-menu-btn-right
    [:i.glyphicon.white.glyphicon-list]]
  :click (fn [btn e]
            (if (dom/has-class? (dom/$ "#project-sidebar") "active")
              (do
                (toggle-menu)
                (toggle-btn))
                  
              (let [made? (update-sidebar)]
                  (go
                    (when (<! made?)
                      (toggle-menu)
                      (toggle-btn)))))))




(defn update-projects
  []
  (go
    (let [projects (<! (model/all-projects orchard.objects.app/db))])))


(defn sidebar-components
  []
  (let [menu  (make-menu [{:title "foobar" :id "boner"} {:title "bizboz" :id "boner"} {:title "wizwoz" :id "boner"}])
        sbtn  (show-btn menu)]
    [menu sbtn]))
