(ns think.objects.wiki-document
  (:use-macros [think.macros :only [defui defgui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.model :as model]
            [think.util.core :as util]
            [think.objects.templates.single-column :as single-column]
            [think.module :as modules]
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
	[:span.btn.btn-danger.header-btn
		{:data-toggle "tooltip"
		 :title "delete this document"}
		[:i.icon-trash.icon-white.header-icon]]
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
  [:span.btn.btn-info.header-btn
   {:data-toggle "tooltip"
    :title "document hyperlink tag"}
   [:i.icon-barcode.icon-white]]
  :click (fn [e]
           (copy-to-clipboard-prompt
             (str "[" (:title @this) "](" (:id @this) ")"))))


(defn left-margin
  [px]
  (str (or px 0) "px"))


(object/object* :wiki-document
  :triggers #{:save :lock-document :unlock-document :ready}
  :behaviors [::save-document ::lock-document ::unlock-document ::ready]
  :locked? true
  :left 0
  :init (fn [this document]
          (let-realised [template (model/get-document (:template document))]
            (let [tpl-obj (object/create (keyword (:type @template)) @template)]
              (object/assoc! this :template tpl-obj)
              (object/parent! this tpl-obj)
              (object/raise tpl-obj :post-init (:id @this))))
          (object/merge! this document {:template (atom {:content [:div]})})
          [:div.document {:style {:left (bound (subatom this :left) left-margin)}}
          [:div.span12
           [:h4
             [:div.pull-left.module-link-label
             		(id-btn this)]
             (:title @this)
             [:div.pull-right
             		(delete-doc-btn this)]]]
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

