(ns orchard.objects.sidebar
  (:require-macros 
    [orchard.macros :refer [defui defgui]])
  (:require 
    [orchard.object :as object]
    [orchard.util.log :refer (log log-obj)]
    [orchard.util.dom  :as dom]
    [orchard.util.core :refer [by-id]]
    [orchard.objects.sidebar.modules-selector :refer [sidebar-modules]]
    [orchard.objects.sidebar.projects-selector :refer [sidebar-projects]]
    [crate.core :as crate]
    [crate.binding :refer [map-bound bound subatom]]
    [orchard.objects.animations :as anim]))

(def DEFAULT-MAX-WIDTH 120)

(def BLOCK-SIZE 30)

(defn set-left
  [left]
  (str (or left 0) "px"))

(defn set-width
  [width]
  (str (or width 0) "px"))

(defui grip
	[this]
	[:div.vertical-grip {:draggable "true"
                       :style {:left (bound (subatom this :width) set-left)}}]
	:dragstart  (fn [e]
                (object/raise this :start-drag))
  :dragend 		(fn [e]
              	(object/raise this :end-drag))
  :drag 			(fn [e]
        			  (object/raise this :width! e)))


(defui sidebar-item [this item]
  (let [{:keys [icon label]} @item]
    [:li.sidebar-tab-item.block-btn
      {:data-toggle "tooltip"
       :data-placement "right"
       :title label
       :class (bound this #(if (= item (:active %))
                              "sidebar-tab-item block-btn current"
                              "sidebar-tab-item block-btn"))}
       icon])
  :click (fn [e]
           (object/raise this :toggle item)
           (object/raise item :toggle e)))


(defui sidebar-tabs
  [this tabs]
  [:ul#sidebar-tabs
    (for [[_ t] tabs]
      (sidebar-item this t))])


(defn active-content
  [active]
  (when active
    (:content @active)))


(object/behavior* ::no-anim-on-drag
                  :triggers #{:start-drag}
                  :reaction (fn [this]
                              (anim/off)))


(object/behavior* ::reanim-on-drop
                  :triggers #{:end-drag}
                  :reaction (fn [this]
                              (anim/on)))


(object/behavior* ::width!
                  :triggers #{:width!}
                  :throttle 5
                  :reaction (fn [this e]
                              (when-not (= 0 (.-clientX e))
                                ; (object/merge! tabs/multi {:left (+ 0 (.-clientX e))})
                                (object/merge! this {:width (- (.-clientX e) 40)
                                                     :max-width (- (.-clientX e) 40)}))))


(object/behavior* ::toggle
                  :triggers #{:toggle}
                  :reaction (fn [this item]
                              ; (log "Toggle sidebar item " item)
                              (if (not= item (:active @this))
                                (do
                                  (anim/on)
                                  ; (log "Setting active sidebar content")
                                  ; (log-obj item)
                                  (object/merge! this {:active item
                                                       :prev (:active @this)})
                                  (object/raise this :open!))
                                (object/raise this :close!))
                              ))


(object/behavior* ::open!
                  :triggers #{:open!}
                  :reaction (fn [this]
                              (object/merge! this {:width (:max-width @this)}) ;; set sidebar width
                              (object/merge! (:wiki-document @orchard.objects.workspace/workspace) {:left (+ 30 (:max-width @this))}) ;; set doc container width
                              ))


(object/behavior* ::close!
                  :triggers #{:close!}
                  :reaction (fn [this]
                              (object/merge! (:wiki-document @orchard.objects.workspace/workspace) {:left 30}) ;; reset doc container width
                              (object/merge! this {:active nil
                                                   :width 0})))


(object/object* ::sidebar
                :tags #{}
                :triggers [:start-drag :end-drag :width! :toggle :open! :close!]
                :behaviors [::no-anim-on-drag ::reanim-on-drop ::width! ::toggle ::open! ::close!]
                :max-width DEFAULT-MAX-WIDTH
                :init (fn [this]
                        [:div#sidebar
                          [:div#sidebar-wrapper
                            (bound (subatom this [:items]) (partial sidebar-tabs this))]
                          [:div#sidebar-conent-wrapper {:style
                                                        {:width    (bound (subatom this :width) set-width)
                                                         :left     "30px"
                                                         :top      "30px"
                                                         :position "relative"}}
                        		[:div.sidebar-content
                        			(bound (subatom this :active) active-content)]
                        		(grip this)]]))


(def sidebar (object/create ::sidebar))


(defn add-item [item]
  (log "Add side bar item" (:label @item))
  (object/update! sidebar [:items] assoc (:order @item) item))


(defn init
  []
  (object/update! sidebar [:items] assoc 0 sidebar-projects 1 sidebar-modules))

