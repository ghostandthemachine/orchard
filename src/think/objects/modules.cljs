(ns think.objects.modules
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [think.model :as model]))


;; TODO: rather than using the (first (:args this)) to get the original record,
;; we should probably keep it around explicitly, or else just save the keys on load.
(object/behavior* ::save-module
                  :triggers #{:save}
                  :reaction (fn [this]
                              (let [original-doc (first (:args @this))
                                    doc-keys     (keys original-doc)
                                    new-doc      (select-keys @this doc-keys)]
                                (model/save-document new-doc))))



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
            (let [msg "Are you sure you want to delete this module?"
                  delete? (js/confirm msg)]
              (when delete?
                (dom/remove (:content @this))))))

(defui edit-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) edit-module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))