(ns think.objects.sidebar
  (:use-macros [redlobster.macros :only [let-realised]]
  						 [think.macros :only [defui defgui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util.dom  :as dom]
            [think.util :as util]
            [think.dispatch :as dispatch]
            [crate.core :as crate]
            [crate.binding :refer [map-bound bound subatom]]
            [think.objects.animations :as anim]
            [redlobster.promise :as p]))

(def DEFAULT-WIDTH 280)

(defui grip
	[this]
	[:div.vertical-grip {:draggable "true"}]
	:dragstart  (fn [e]
                (object/raise this :start-drag))
  :dragend 		(fn [e]
              	(object/raise this :end-drag))
  :drag 			(fn [e]
        			  (object/raise this :width! e)))


(defn set-width
	[width]
	(str (or width 0) "px"))


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


(object/behavior* ::add!
  :triggers #{:add!}
  :reaction (fn [this]
              (log "Add sidebar to workspace")
              (dom/append (dom/$ "body") (:content @this))))


(object/object* ::sidebar
                :tags #{}
                :triggers [:start-drag :end-drag :width! :add!]
                :behaviors [::no-anim-on-drag ::reanim-on-drop ::width! ::add!]
                :max-width DEFAULT-WIDTH
                :init (fn [this]
                        [:div#sidebar
                        	[:div.container {:style {:width (bound (subatom this :width) set-width)}}
                        		[:div.content
                        			(bound (subatom this :active) active-content)]
                        		(grip this)]]))


; (def sidebar (object/create ::sidebar))