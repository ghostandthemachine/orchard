(ns think.objects.modules.form
  (:use [think.util.core :only [log log-obj uuid]])
  (:use-macros [dommy.macros :only [sel]]
               [think.macros :only [defui]]
               [redlobster.macros :only [let-realised defer-node]])
  (:require [think.object :as object]
            [think.objects.modules :refer [module-btn-icon module-btn]]
            [think.util.core :refer [bound-do]]
            [think.util.dom :as dom]
            [think.model :as model]
            [redlobster.promise :as p]
            [crate.core :as crate]
            [crate.binding :refer [bound subatom]]
            [clojure.string :as string]
            [dommy.core :as dom]))


(defn render-form
  [this])


(defn render-module
  [this])


(object/object* :form-module
                :tags #{}
                :triggers #{:save :delete}
                :behaviors [:think.objects.modules/save-module :think.objects.modules/delete-module]
                :mode :present
                :editor nil
                :type nil
                :init (fn [this record]
                        (log "init form module")
                        (log-obj this)
                        (log-obj record)
                        (object/merge! this record)
                        (bound-do (subatom this :mode) (partial render-module this))
                        [:div.span12.module.media-module {:id (str "form-module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-element (render-form this)]]))
