(ns think.objects.sidebar.modules
  (:use-macros [redlobster.macros :only [let-realised]]
  						 [think.macros :only [defui defgui]])
  (:require [think.object :as object]
            [think.objects.sidebar :as sidebar]
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


(object/object* ::sidebar.modules
                :triggers #{}
                :behaviors [::toggle]
                :label "modules"
                :order 1
                :init (fn [this]
                        [:div.modules-content
                          ; (bound (subatom this [:items]) (partial sidebar-modules this))
                          ; (modules this [(atom {:label "Markdown"})])
                          ]))


(def sidebar-modules (object/create ::sidebar.modules))


(sidebar/add-item sidebar-modules)


(defn add-item [item]
  (object/update! sidebar-modules [:items] assoc (:order @item) item))
