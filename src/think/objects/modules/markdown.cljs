(ns think.objects.modules.markdown
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.objects.module :as module]
            [think.util.log :refer [log]]
            [crate.binding :refer [bound subatom]]))


(defui module-btn
  [this]
  [:div.module-btn]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))

(defui render-present
  [this]
  [:div.markdown-module-content
    (crate/raw (js/markdown.toHTML (:text @this)))])


(defui render-edit
  [this]
  [:div.markdown-module-editor
    [:h3 "This is an editor"]])


(defn render-module
  [this _]
    (log "module mode: " (:mode @this))
    (case (:mode @this)
      :present (render-present this)
      :edit    (render-edit this)))



(object/object* :markdown-module
                :tags #{}
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        [:div.module.markdown-module
                          [:div.module-tray (module-btn this)]
                          [:div.module-element (bound (subatom this :mode) (partial render-module this))]]))
