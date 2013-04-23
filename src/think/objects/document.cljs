(ns think.objects.document
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.model :as model]
            [crate.binding :refer [subatom bound]]
            [think.util.log :refer [log log-obj]]))


(defui render-template
  [this template]
  [:div.document-content
    (object/->content template)])


(object/behavior* ::save-document
                  :triggers #{:save}
                  :reaction (fn [this]
                              (log "saving document...")
                              (log-obj @this)
                              (let [original-doc (first (:args @this))
                                    doc-keys     (keys original-doc)
                                    mod-ids      (map :id (:modules (:template @this)))
                                    _ (log "mod-ids:" mod-ids)
                                    new-doc      (select-keys @this doc-keys)
                                    new-doc      (assoc-in new-doc [:template :modules] mod-ids)]
                                (model/save-document new-doc))))

;(let [modules (map #(db/get-doc (:document-db* @model) %)
;                           (get-in doc [:template :modules]))]
;          (when-realised modules
;                         (log "modules realised...")
;                         (p/realise res-promise (assoc-in doc [:template :modules]
;                                                          (map deref modules)))))
;
(object/object* :document
                :triggers #{:save}
                :behaviors [::save-document]
                :init (fn [this document]
                        (log "Init document with document " document)
                        (let [template (:template document)
                              tpl-obj (object/create (keyword (:type template)) template this)]
                          (object/merge! this
                            (assoc document :template tpl-obj))

                          [:div.container-fluid.document
                            [:div.row-fluid
                              [:div.span12
                                [:h4 (:title @this)]]]
                            (bound (subatom this [:template])
                              (partial render-template this))])))
