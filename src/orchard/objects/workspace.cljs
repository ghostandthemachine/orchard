(ns orchard.objects.workspace
  (:require [orchard.object          :as object]
            [orchard.objects.sidebar :as sidebar]
            [orchard.dispatch        :as dispatch]
            [orchard.util.core       :refer (has?)]
            [orchard.util.dom        :as dom]
            [orchard.util.log        :refer (log log-obj)]
            [orchard.util.cljs       :refer [->dottedkw]]
            [crate.binding           :refer [map-bound bound subatom]])
  (:require-macros [orchard.macros :refer [defui]]))


(def default-width 950)


(defui render-document
  [this doc]
  (object/->content doc))


(object/behavior* ::add-sidebar
                  :triggers #{:add-sidebar}
                  :reaction (fn [this sidebar]
                                (object/assoc! this :sidebar sidebar)))


; TODO:
; Change this to a more generic show-content, which just takes a UI element
; and loads it onto the main page.  (The ready handler(s) for the enclosed
; content should handle any initialization necessary.)

(object/behavior* ::show-page
                  :triggers #{:show-page}
                  :reaction (fn [this obj]
                              (dispatch/fire :page-loading obj)
                              (let [workspace$ (dom/$ "#workspace")
                                    active     (:wiki-page @this)]
                                ; TODO: Nothing to specific modules should be happening here...
                                (orchard.objects.modules.tiny-mce/clear-editors!)

                                (when active
                                  (dom/remove (:content @active)))

                                (object/assoc! this
                                  :wiki-page obj
                                  :current-project (or (:project @obj) "No Project"))

                                (object/raise obj :ready)
                                (dispatch/fire :page-loaded obj))))


(defn active-content [active]
  (when active
    (object/->content active)))


(defn ->width [width]
  (str (or width 0) "px"))


(defn render-wiki-page
  [wiki-page]
  (when wiki-page
  ; (log-obj (clj->js @wiki-page))
    (:content @wiki-page)))


(defn render-sidebar
  [sidebar]
  (when sidebar
    (:content @sidebar)))


(defn left-offset
  [offset]
  (str (or offset 0)) "px")


(object/object* ::workspace
                :triggers  #{:show-page :add-sidebar}
                :behaviors [::show-page ::add-sidebar]
                :width 0
                :sidebar sidebar/sidebar
                :transients '()
                :max-width default-width
                :init (fn [this]
                        [:div#workspace
                          ; {:style {:left (bound (subatom orchard.objects.sidebar/sidebar :width) left-offset)}}
                          (bound (subatom this :wiki-page) render-wiki-page)]))

(def workspace (object/create ::workspace))
(dom/replace-with (dom/$ :#workspace) (:content @workspace))


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

