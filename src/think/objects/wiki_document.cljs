(ns think.objects.wiki-document
  (:use-macros [think.macros :only [defui defgui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.model :as model]
            [think.util.core :as util]
            [think.util.dom :refer [set-frame-title]]
            [think.objects.templates.single-column :as single-column]
            [think.module :as modules]
            [think.dispatch :as dispatch]
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


(object/behavior* ::lock-document
  :triggers #{:lock-document}
  :reaction (fn [this]
              (log "Present Mode")
              (object/update! this [:locked?]
              	(fn [_]
              		true))
              (.css (js/$ ".module")
  							"background-color"
  							"rgb(255, 255, 255)")))


(object/behavior* ::unlock-document
  :triggers #{:unlock-document}
  :reaction (fn [this]
              (log "Edit Mode")
              (object/update! this [:locked?]
              	(fn [_]
              		false))
              (.css (js/$ ".module")
  							"background-color"
  							"rgb(247, 247, 247)")))


(object/behavior* ::ready
  :triggers #{:ready}
  :reaction (fn [this]
              (.tooltip
                (js/$ ".header-label")
                (clj->js
                  {:delay 100
                   :placement "bottom"}))
              (object/raise (:template @this) :ready)))


(defui delete-doc-btn
	[this]
	[:span.header-btn
		{:data-toggle "tooltip"
		 :title "delete this document"}
		[:i.icon-trash.header-icon]]
	:click (fn [e]
            (let [msg "Are you sure you want to delete this document?\nThis will permanently delete this document."
                  delete? (js/confirm msg)]
              (when delete?
                (model/delete-document @this)
                (think.objects.app/open-document :home)))))



(defn copy-to-clipboard-prompt
  [text]
  (.prompt js/window "Copy to clipboard: cmd + c, Enter", text))


(defui id-btn
  [this]
  [:span.header-btn
   {:data-toggle "tooltip"
    :title "document hyperlink tag"}
   [:i.icon-barcode]]
  :click (fn [e]
           (copy-to-clipboard-prompt
             (str "[" (:title @this) "](" (:id @this) ")"))))


(object/object* :wiki-document
  :triggers #{:save :lock-document :unlock-document :ready}
  :behaviors [::save-document ::lock-document ::unlock-document ::ready]
  :locked? true
  :title ""
  :init (fn [this document]
          (let-realised [template (model/get-document (:template document))]
            (let [tpl-obj (object/create (keyword (:type @template)) @template)]
              (object/assoc! this :template tpl-obj)
              (object/parent! this tpl-obj)
              (object/raise tpl-obj :post-init (:id @this))))
          (object/merge! this document {:template (atom {:content [:div]})})
          [:div.document
            [:div.row-fluid
              [:div
                (bound (subatom this [:template])
                  (partial render-template this))]
              [:div.pull-left
                (id-btn this)
                (delete-doc-btn this)]]]))


(dispatch/react-to #{:open-document}
  (fn [_ & [document]]
    (log (:title document))
    (set-frame-title (:title document))))


(defn wiki-doc
  [title tpl]
  (model/save-document
    {:type :wiki-document
     :id (util/uuid)
     :title title
     :template (:id tpl)}))



