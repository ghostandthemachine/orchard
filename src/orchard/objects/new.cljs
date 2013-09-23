(ns orchard.objects.new
  (:require-macros
    [orchard.macros :refer [defui]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [orchard.object :as object]
    [cljs.core.async :refer [chan >! <! timeout]]
    [orchard.model :as model]
    [dommy.core :as dommy]
    [orchard.objects.templates.single-column :as single-column]
    [orchard.objects.modules.markdown :as markdown]
    [orchard.objects.modules.tiny-mce :as tiny]
    [orchard.objects.wiki-document :as wiki-doc]
    [orchard.objects.workspace :as workspace]
    [orchard.module :as modules]
    [crate.binding :refer [subatom bound]]
    [orchard.util.log :refer (log log-obj)]
    [orchard.util.dom :as dom]))


;TODO: once finer grain pouch queries are working the templates should be loaded not hard coded
(defui new-document-form
  [this]
  [:div.offset3.span6.new-document-form
    [:label "Title"]
    [:input#new-document-title.span6 {:type "text" :placeholder "New page"}]
    [:a#create-doc-btn.btn.btn-primary "Create"]])


(object/object* :new-document
  :triggers #{}
  :behaviors []
  :init (fn [this]
          [:div.document
           [:div.row-fluid
            [:div.span12
             [:h4 "Create a new Wiki Document"]]]
            [:div.row-fluid
              [:div.new-content.new-document-content
                (new-document-form this)]]]))


(def new-document (object/create :new-document))


(defn load-new-doc
  []
  (object/raise orchard.objects.workspace/workspace :show-document new-document))


(defn create-document
  [db title]
  (go
    (let [mod-doc  (<! (tiny/tiny-mce-doc db))
          tpl-doc  (<! (single-column/single-column-template-doc db mod-doc))
          wiki-doc (<! (wiki-doc/wiki-doc db :title title :template tpl-doc))
          doc      (<! (model/load-object db (:id wiki-doc)))]
      (object/raise workspace/workspace :show-document doc))))


(dommy/listen! [(dom/$ :body) :.new-document-form :a] :click
  (fn [e]
    (.preventDefault e)
    (let [$title-input (dom/$ :#new-document-title)
          title        (.-value $title-input)]
      (create-document orchard.objects.app.db title))))
