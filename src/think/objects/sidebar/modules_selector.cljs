(ns think.objects.sidebar.modules-selector
  (:require-macros 
    [think.macros :refer [defui defgui]])
  (:require 
    [think.object :as object]
    [think.util.log :refer (log log-obj)]
    [crate.binding :refer [map-bound bound subatom]]))


(defui module-item
  [item]
  [:li.modules-content-item
    item]
  :click (fn [e]
           (log "click module ")
           ))


(defui render-modules
  [mods]
  [:ul.modules-content-list
    (for [m mods]
      (module-item m))])


(defui modules-icon
  []
  [:i.icon-tasks])

(defui templates-item
  []
  [:h2.sidebar-content-title "Templates"])

(defui modules-item
  []
  [:h2.sidebar-content-title "Modules"])

(defui elements-item
  []
  [:h2.sidebar-content-title "Elements"])


(defn add-item [this item]
  (object/update! this [:items] conj item))

(object/object* ::sidebar.modules
                :triggers #{}
                :behaviors [::toggle]
                :label "modules"
                :items []
                :icon (modules-icon)
                :init (fn [this]
                        (doseq [item [(templates-item) (modules-item) (elements-item)]]
                          (add-item this item))
                        [:div.modules-content
                          (bound (subatom this [:items]) render-modules)]))


(def sidebar-modules (object/create ::sidebar.modules))



