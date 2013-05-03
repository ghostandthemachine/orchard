(ns think.objects.modules
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.util.dom :as dom]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [think.model :as model]))


(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true
     :matchBrackets true
     :viewportMargin js/Infinity}))


(defn edit-module-btn-icon
  [mode]
  (if (= mode :present)
    "icon-pencil module-btn"
    "icon-ok module-btn"))


(defui delete-btn
  [this]
  [:i.icon-trash.module-btn]
  :click (fn [e]
            (log "Delete module btn hit " (:id @this))
            (log-obj @this)
            (let [msg "Are you sure you want to delete this module?"
                   delete? (js/confirm msg)]
                (when delete?
                  (object/raise (object/parent this) :remove-module this)))))

(defui edit-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) edit-module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))



;; TODO: rather than using the (first (:args this)) to get the original record,
;; we should probably keep it around explicitly, or else just save the keys on load.
(object/behavior* ::save-module
  :triggers #{:save}
  :reaction (fn [this]
              (log "Save module")
              (let [original-doc (first (:args @this))
                    doc-keys     (keys original-doc)
                    new-doc      (select-keys @this doc-keys)]
                (model/save-document new-doc))))


