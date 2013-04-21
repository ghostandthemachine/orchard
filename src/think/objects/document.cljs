(ns think.objects.document
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.model :as model]
            [crate.binding :refer [subatom bound]]
            [think.util.log :refer [log]]))


(defui render-template
  [this template]
  [:div.document-content
    (object/->content template)])


(object/object* :document
                :triggers #{}
                :behaviors []
                :init (fn [this document]
                        (log "Init document with document " document)
                        (let [template (:template document)
                              tpl-obj (object/create (keyword (:type template)) template)]
                          (object/merge! this
                            (assoc document :template tpl-obj))

                          [:div.container-fluid.document
                            [:div.row-fluid
                              [:div.span12
                                [:h4 (:title @this)]]]
                            (bound (subatom this [:template])
                              (partial render-template this))])))