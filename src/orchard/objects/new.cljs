(ns orchard.objects.new
  (:require-macros
    [orchard.macros :refer [defui]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! timeout]]
    [orchard.object :as object]
    [orchard.objects.templates.single-column :as single-column]
    [orchard.objects.modules.editor :as editor]
    [orchard.objects.wiki-page :as wiki-page]
    [orchard.objects.workspace :as workspace]
    [crate.binding :refer [subatom bound]]
    [orchard.util.log :refer (log log-obj)]
    [orchard.util.dom :as dom]))


(defn create-new-document
  [db title]
  (go
    (let [mod-doc   (<! (editor/editor-doc db))
          tpl-doc   (<! (single-column/single-column-template-doc db mod-doc))
          wiki-page (<! (wiki-page/wiki-page db :title title :template tpl-doc))
          obj (object/create (:type wiki-page) wiki-page)]
      (object/raise workspace/workspace :show-page obj))))


(defui create-button
  []
  [:a#create-doc-btn.btn.btn-primary "Create"]
  :click (fn [e]
           (.preventDefault e)
           (let [$title-input (dom/$ :#new-document-title)
                 title        (.-value $title-input)
                 $editor-input (.getElementById js/document "editor-check")]
             (create-new-document orchard.objects.app.db title))))


;TODO: once finer grain pouch queries are working the templates should be loaded not hard coded
(defui new-document-form
  [this]
  [:form.new-document-form
   [:label "Title"]
   [:input#new-document-title.span6 {:type "text" :placeholder "New page"}]
   [:br]
   (create-button)])


(object/object* :new-document
  :triggers #{}
  :behaviors []
  :init (fn [this]
          [:div.container-fluid
            [:div.span9.offset3
              [:div.row-fluid
                [:h4 "Create a new Wiki Document"]]
              [:div.row-fluid
                [:div.new-content.new-document-content
                  (new-document-form this)]]]]))


(def new-document (object/create :new-document))


(defn load-new-doc
  []
  (object/raise orchard.objects.workspace/workspace :show-page new-document))