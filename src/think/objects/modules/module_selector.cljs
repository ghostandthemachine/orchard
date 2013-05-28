(ns think.objects.modules.module-selector
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [redlobster.promise :as p]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.objects.modules :refer [module-btn module-btn-icon]]
            [think.objects.modules :as modules]
            [think.objects.modules.markdown :as md]
            [think.objects.modules.media :as media]
            [think.objects.modules.visualizer :as viz]
            [think.objects.modules.html :as html]
            [think.util :refer [bound-do uuid]]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))


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
                (for [icon [(modules/create-module-icon this
                              md/icon
                              md/create-module)
                            (modules/create-module-icon this
                              html/icon
                              html/create-module)
                            (modules/create-module-icon this
                              media/icon
                              media/create-module)
                            (modules/create-module-icon this
                              viz/icon
                              viz/create-module)]]
                  [:div.span1
                    icon])]]]))


(def module-selector-module (object/create :module-selector-module))

