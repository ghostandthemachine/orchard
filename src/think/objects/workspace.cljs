(ns think.objects.workspace
  (:require [think.object :as object]
            [think.objects.sidebar :as sidebar]
            [think.util.core :refer (has?)]
            [think.util.dom :as dom]
            [think.util.log :refer (log log-obj)]
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
                                (think.objects.modules.tiny-mce/clear-editors!)
                                (when active
                                  (dom/remove (:content @active)))
                                (object/assoc! this
                                  :wiki-document doc-obj
                                  :current-project (or (:project @doc-obj) "No Project"))
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


(defn left-offset
  [offset]
  (str (or offset 0)) "px")


(object/object* ::workspace
                :triggers  #{:show-document :add-sidebar}
                :behaviors [::show-document ::add-sidebar]
                :width 0
                ; :channels {:event []}
                :sidebar sidebar/sidebar
                :transients '()
                :max-width default-width
                :init (fn [this]
                        [:div#workspace
                          [:div.document-container
                            {:style {:left (bound (subatom think.objects.sidebar/sidebar :width) left-offset)}}
                            (bound (subatom this :wiki-document) render-wiki-doc)]]))

(def workspace (object/create ::workspace))
(dom/append (dom/$ :#container) (:content @workspace))


; (defn channel-type-supported?
;   [type]
;   (has?
;     (keys (:channels @workspace))
;     type))


; (defn create-chan
;   "Creates a new event channel and adds it to the workspace event chan seq."
;   [chan-type]
;   (if (channel-type-supported? chan-type)
;     (let [c (chan)]
;       (object/update! workspace [:channels chan-type] conj c)
;       c)
;     (log "Channel type: " chan-type " not suuported for workspace object")))

