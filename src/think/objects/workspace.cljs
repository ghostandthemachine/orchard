(ns think.objects.workspace
  (:require [think.object :as object]
            [think.objects.canvas :as canvas]
            [think.objects.document :as document]
            [think.util.dom :as dom]
            [think.util.log :refer [log]]
            [think.util.cljs :refer [->dottedkw]]
            [think.objects.templates.single-column :as sctmpl]
            [crate.binding :refer [map-bound bound subatom]])
  (:require-macros [think.macros :refer [defui]]))

(def default-width 950)

(defui render-document
  [this doc]
  (object/->content doc))


(object/behavior* ::show-document
                  :triggers #{:show-document}
                  :reaction (fn [this doc]
                              (object/merge! this {:document doc})
                              (dom/append (:content @this) (:content @doc))))


(defn active-content [active]
  (when active
    (object/->content active)))

(defn ->width [width]
  (str (or width 0) "px"))

(object/object* ::workspace
                :triggers  #{:show-document}
                :behaviors [::show-document]
                :width 0
                :transients '()
                :max-width default-width
                :init (fn [this]
                      [:div#workspace.container-fluid]))

(def workspace (object/create ::workspace))

(canvas/add! workspace)


; (defn add-module [module]
;   (object/update! workspace [:modules] conj module))

; (defn record->module
;   [record]
;   (module/create-module record))


; (defn create
;   [record]
;   (object/merge! workspace
;     ; (assoc record :template (sctmpl/record->template record))
;     record
;     ))

