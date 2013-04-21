(ns think.objects.document-loader
  (:use-macros [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.objects.workspace :as workspace]
            [think.model :as model]
            [think.util.log :refer [log]]
            [crate.binding :refer [bound subatom]]))


(object/behavior* ::load-document
                  :triggers #{:load-document}
                  :reaction (fn [this k]
                              (log "Loading doc " k)
                              (let-realised [doc (model/get-document k)]
                                (object/raise workspace/workspace :load-document @doc))))


(object/object* ::loader
                :triggerss #{:load-document}
                :behaviors [::load-document]
                :init (fn [this] ))


(def loader (object/create ::loader))
