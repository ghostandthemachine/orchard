(ns orchard.objects.wiki-page
  (:require-macros
    [orchard.macros :refer [defui defgui]]
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require
    [cljs.core.async :refer [chan >!! <!! thread timeout]]
    [orchard.object :as object]
    [orchard.model :as model]
    [orchard.util.core :as util]
    [orchard.util.dom :refer [set-frame-title]]
    [orchard.objects.templates.single-column :as single-column]
    [orchard.module :as modules]
    [orchard.dispatch :as dispatch]
    [crate.binding :refer [subatom bound]]
    [orchard.util.log :refer (log log-obj)]))


(defui render-template
  [this template]
  [:div.document-content
    (object/->content template)])


(object/behavior* ::save-document
  :triggers #{:save}
  :reaction (fn [this]
              (model/save-object! orchard.objects.app.db (:id @this) (assoc
                                   (select-keys @this [:id :rev :type :title])
                                   :template (:id @(:template @this))))))


(object/behavior* ::lock-document
  :triggers #{:lock-document}
  :reaction (fn [this]
              (object/update! this [:locked?]
              	(fn [_]
              		true))
              (.css (js/$ ".module")
  							"background-color"
  							"rgb(255, 255, 255)")))


(object/behavior* ::unlock-document
  :triggers #{:unlock-document}
  :reaction (fn [this]
              (object/update! this [:locked?]
              	(fn [_]
              		false))))


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
                (orchard.objects.app/open-document orchard.objects.app.db :home)))))



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


(object/object* :wiki-page
  :triggers #{:save :lock-document :unlock-document :ready}
  :behaviors [::save-document ::lock-document ::unlock-document ::ready]
  :locked? true
  :title ""
  :init (fn [this document]
          (go
            ; TODO: change this to use model/load-object...
            (let [template (<! (model/get-object orchard.objects.app.db (:template document)))
                  tpl-obj  (object/create (keyword (:type template)) template)]
              (object/assoc! this :template tpl-obj)
              (object/parent! this tpl-obj)
              (object/raise tpl-obj :post-init (:id @this))))
          (object/merge! this document {:template (atom {:content [:div]})})
          [:div.document
            [:div.row-fluid
              [:div
                (bound (subatom this [:template])
                  (partial render-template this))]
              ; [:div.pull-left
              ;   (id-btn this)
              ;   (delete-doc-btn this)]
                ]]))


(dispatch/react-to #{:open-document}
  (fn [event-id document]
    (set-frame-title (:title @document))))


(defn wiki-page
  [db & args]
  (let [new-doc (merge
                  {:type :wiki-page
                   :id   (util/uuid)}
                  (apply hash-map args))]
  (model/save-object! db (:id new-doc) (assoc new-doc :template (:id (:template new-doc))))))

