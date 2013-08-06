(ns think.objects.sidebar.projects-selector
  (:require-macros
    [think.macros :refer (defui defgui)])
  (:require 
    [think.object :as object]
    [think.util.log :refer (log log-obj)]
    [crate.binding :refer [map-bound bound subatom]]))


(defui project-item [this item]
  (let [{:keys [label]} @item]
    [:li {:class (bound this #(when (= item (:active %))
                                "current"))}
                        label])
  :click (fn [e]
           (log "click project " (:label @item))
           ))


(defui projects
  [this mods]
  [:ul#projects-sidebar
    (for [[_ t] mods]
      (project-item this t))])


(defui projects-icon
  []
  [:i.icon-folder-open])

(object/object* ::sidebar.projects-selector
                :triggers #{}
                :behaviors [::toggle]
                :label "projects"
                :icon (projects-icon)
                :order 1
                :init (fn [this]
                        [:div.projects-content
                          ; (bound (subatom this [:items]) (partial sidebar-projects this))
                          [:div.row-fluid.item {:draggable "true"}
                            [:h4 "Working"]]
                          ]))


(def sidebar-projects (object/create ::sidebar.projects-selector))


; (sidebar/add-item sidebar-projects)


(defn add-item [item]
  (object/update! sidebar-projects [:items] assoc (:order @item) item))
