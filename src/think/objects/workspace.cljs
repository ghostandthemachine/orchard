(ns think.objects.workspace
  (:require [think.object :as object]
            [think.objects.canvas :as canvas]
            [think.util.dom :as dom]
            [think.util.log :refer [log log-obj]]
            [think.util.cljs :refer [->dottedkw]]
            [crate.binding :refer [map-bound bound subatom]])
  (:require-macros [think.macros :refer [defui]]))

(def default-width 950)


(defui render-document
  [this doc]
  (object/->content doc))


(object/behavior* ::show-document
                  :triggers #{:show-document}
                  :reaction (fn [this doc-obj]
                              (let [workspace$ (dom/$ "#workspace")
                                    active     (:wiki-document @this)]
                                (when active
                                  (dom/remove (:content @active)))                                
                                (object/assoc! this :wiki-document doc-obj)
	   	  											  (object/raise doc-obj :ready))))


(defn active-content [active]
  (when active
    (object/->content active)))


(defn ->width [width]
  (str (or width 0) "px"))


(defn render-wiki-doc
  [wiki-document]
  (log "render-wiki-doc")
  (log-obj wiki-document)
  (when wiki-document
    (:content @wiki-document)))


(object/object* ::workspace
                :triggers  #{:show-document}
                :behaviors [::show-document]
                :width 0
                :transients '()
                :max-width default-width
                :init (fn [this]
                        [:div#workspace.container-fluid
                          (bound (subatom this :wiki-document) render-wiki-doc)]))


(def workspace (object/create ::workspace))


(canvas/add! workspace)
