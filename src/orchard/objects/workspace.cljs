(ns orchard.objects.workspace
  (:require [orchard.object          :as object]
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
    (:content @wiki-page)))


(defn left-offset
  [offset]
  (str (or offset 0)) "px")


(object/object* ::workspace
                :triggers  #{:show-page}
                :behaviors [::show-page]
                :width 0
                :transients '()
                :max-width default-width
                :init (fn [this]
                        [:div#workspace
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

