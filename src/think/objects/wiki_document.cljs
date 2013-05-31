(ns think.objects.wiki-document
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.model :as model]
            [think.util :as util]
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
              (log "Save document")
              (model/save-document (assoc
                                    (select-keys @this [:id :rev :type :title])
                                    :template (:id @(:template @this))))))


(object/object* :wiki-document
  :triggers #{:save}
  :behaviors [::save-document]
  :init (fn [this document]
          (log "save wiki-document")
          (let-realised [template (model/get-document (:template document))]
            (log "template for wiki doc")
            (log-obj @template)
            (let [tpl-obj (object/create (keyword (:type @template)) @template)]
              (object/assoc! this :template tpl-obj)
              (object/raise tpl-obj :post-init (:id @this))))
          (object/merge! this document {:template (atom {:content [:div]})})
          [:div.document
           [:div.row-fluid
            [:div.span12
             [:h4 (:title @this)]]]
            [:div.row-fluid
              (bound (subatom this [:template])
                (partial render-template this))]]))


(defn wiki-doc
  [title tpl]
  (model/save-document
    {:type :wiki-document
     :id (util/uuid)
     :title title
     :template (:id tpl)}))

