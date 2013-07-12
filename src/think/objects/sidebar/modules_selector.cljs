(ns think.objects.sidebar.modules-selector
  (:require-macros [redlobster.macros :refer [let-realised]]
  						     [think.macros :refer [defui defgui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [map-bound bound subatom]]))


(defui module-item [this item]
  (let [{:keys [label]} @item]
    [:li {:class (bound this #(when (= item (:active %))
                                "current"))}
                        label])
  :click (fn [e]
           (log "click module " (:label @item))
           ))


(defui modules
  [this mods]
  [:ul#modules-sidebar
    (for [[_ t] mods]
      (module-item this t))])


(defui modules-icon
  []
  [:i.icon-tasks])

(object/object* ::sidebar.modules-selector
                :triggers #{}
                :behaviors [::toggle]
                :label "modules"
                :icon (modules-icon)
                :order 1
                :init (fn [this]
                        [:div.modules-content
                          ; (bound (subatom this [:items]) (partial sidebar-modules this))
                          [:div.row-fluid.item {:draggable "true"}
                            [:h4 "Working"]]
                          ]))


(def sidebar-modules (object/create ::sidebar.modules-selector))


; (sidebar/add-item sidebar-modules)


(defn add-item [item]
  (object/update! sidebar-modules [:items] assoc (:order @item) item))
