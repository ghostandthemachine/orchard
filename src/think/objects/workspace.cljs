(ns think.objects.workspace
  (:require [think.object :as object]
            [think.objects.canvas :as canvas]
            [think.objects.module :as module]
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



(object/behavior* ::load-document
                  :triggers #{:load-document}
                  :reaction (fn [this doc]
                              (log "load-document" doc)
                              (let [doc-obj (document/create! doc)]
                                (object/merge! this {:document doc-obj})
                                (dom/append (:content @this) (:content @doc-obj)))))


(defn active-content [active]
  (when active
    (object/->content active)))

(defn ->width [width]
  (str (or width 0) "px"))

(object/object* ::workspace
                :triggers  #{:load-document}
                :behaviors [::load-document]
                :width 0
                :transients '()
                :max-width default-width
                :init (fn [this]
                      [:div#workspace
                        [:h3 "workspace"]]))

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

