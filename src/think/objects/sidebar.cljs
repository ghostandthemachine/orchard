(ns think.objects.sidebar
  (:require-macros [redlobster.macros :refer [let-realised]]
  						     [think.macros :refer [defui defgui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util.dom  :as dom]
            [think.util.core :as util]
            [think.objects.sidebar.modules-selector :refer [sidebar-modules]]
            [think.objects.sidebar.projects-selector :refer [sidebar-projects]]
            [crate.core :as crate]
            [crate.binding :refer [map-bound bound subatom]]
            [think.objects.animations :as anim]
            [redlobster.promise :as p]))

(def DEFAULT-MAX-WIDTH 100)

(defn set-left
  [left]
  (str (or left 0) "px"))

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


; (defui sidebar-item [this item]
;   (let [{:keys [label]} @item]
;     [:li {:class (bound this #(if (= item (:active %))
;                                 "rotate-right current"
;                                 "rotate-right"))}
;                         label])
;   :click (fn [e]
;            (object/raise this :toggle item)
;            (object/raise item :toggle e)))


(defui sidebar-item [this item]
  (let [{:keys [icon label]} @item]
    [:li.btn.sidebar-tab-item
    {:data-toggle "tooltip"
     :data-placement "right"
     :title label}
     icon])
  :click (fn [e]
           (object/raise this :toggle item)
           (object/raise item :toggle e)))


(defui sidebar-tabs
  [this tabs]
  [:ul#sidebar-tabs
    (for [[_ t] tabs]
      (sidebar-item this t))])


(defn set-width
	[width]
	(str (or width 0) "px"))


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
                              (object/merge! (:wiki-document @think.objects.workspace/workspace) {:left (:max-width @this)}) ;; set doc container width
                              ))


(object/behavior* ::close!
                  :triggers #{:close!}
                  :reaction (fn [this]
                              (object/merge! (:wiki-document @think.objects.workspace/workspace) {:left 0}) ;; reset doc container width
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
                                                        {:width (bound (subatom this :width) set-width)
                                                         :left "30px"
                                                         :top "-34px"
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
  (add-item sidebar-projects)
  (add-item sidebar-modules)
  )

