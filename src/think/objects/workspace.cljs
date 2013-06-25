(ns think.objects.workspace
  (:require [think.object :as object]
            [think.objects.canvas :as canvas]
            [think.objects.sidebar :as sidebar]
            [think.util.dom :as dom]
            [think.util.log :refer [log log-obj]]
            [think.util.cljs :refer [->dottedkw]]
            [crate.binding :refer [map-bound bound subatom]])
  (:require-macros [think.macros :refer [defui]]))

(def default-width 950)


(defui render-document
  [this doc]
  (object/->content doc))


(object/behavior* ::add-sidebar
                  :triggers #{:add-sidebar}
                  :reaction (fn [this sidebar]
                                (object/assoc! this :sidebar sidebar)))


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
  (when wiki-document
  ; (log-obj (clj->js @wiki-document))
    (:content @wiki-document)))


(defn render-sidebar
  [sidebar]
  (when sidebar
    (:content @sidebar)))


(object/object* ::workspace
                :triggers  #{:show-document :add-sidebar}
                :behaviors [::show-document ::add-sidebar]
                :width 0
                :sidebar sidebar/sidebar
                :transients '()
                :max-width default-width
                :init (fn [this]
                        [:div#workspace.row-fluid
                          [:div
                            (bound (subatom this :sidebar) render-sidebar)]
                          [:div.container-fluid.document-container
                            (bound (subatom this :wiki-document) render-wiki-doc)]]))

(def workspace (object/create ::workspace))
(canvas/add! workspace)
