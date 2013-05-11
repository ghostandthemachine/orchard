(ns think.objects.modules.module-selector
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [redlobster.promise :as p]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.objects.modules :as modules]
            [think.objects.modules.markdown :as md]
            [think.objects.modules.html :as html]
            [think.util :refer [bound-do uuid]]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))


(defn module-btn-icon
  [mode]
  (if (= mode :present)
    "icon-pencil module-btn"
    "icon-ok module-btn"))

(defui module-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))


(object/object* :module-selector-module
  :tags #{}
  :triggers #{}
  :behaviors []
  :id (uuid)
  :init (fn [this]
          [:div.span12.module.module-selector-module {:id (str "module-" (:id @this))}
            [:div.module-tray (module-btn this)]
            [:div.module-content.module-selector-module-content
              [:div.row-fluid
                (for [icon [(modules/render-icon
                              this md/icon
                              md/create-module
                              (md/markdown-doc))
                            (modules/render-icon
                              this html/icon
                              html/create-module
                              (html/html-doc))]]
                  [:div.span1
                    icon])]]]))


(def module-selector-module (object/create :module-selector-module))

