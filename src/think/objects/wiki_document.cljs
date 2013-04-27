(ns think.objects.wiki-document
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.model :as model]
            [think.objects.templates.single-column :as single-column]
            [think.objects.modules :as modules]
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
                              (let [doc-keys     (:db-keys @this)
                                    mod-ids      (map #(:id @%) (:modules @(:template @this)))
                                    new-doc      (select-keys @this doc-keys)
                                    obj-tpl      (dissoc @(:template new-doc) :args)
                                    new-tpl      (select-keys (merge obj-tpl {:modules mod-ids})
                                                    (:db-keys obj-tpl))
                                    new-doc      (dissoc (assoc new-doc :template new-tpl) :args)]
                                (log-obj new-doc)
                                (doseq [k (:db-keys @this)
                                        :let
                                        [v (k @this)]]
                                  (log (str k))
                                  (log (type v))
                                  (log-obj v))
                                (model/save-document new-doc))))

(object/object* :wiki-document
                :triggers #{:save}
                :behaviors [::save-document]
                :init (fn [this document]
                        (let [template (:template document)
                              tpl-obj  (object/create (keyword (:type template)) template this)]
                          (object/merge! this (assoc document :template tpl-obj :db-keys (keys document)))
                          [:div#document
                            [:div.row-fluid
                              [:div.span12
                                [:h4 (:title @this)]]]
                            (bound (subatom this [:template])
                              (partial render-template this))

  ])))


