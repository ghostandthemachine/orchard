(ns think.objects.new
  (:require-macros 
    [think.macros :refer [defui]]
    [cljs.core.async.macros :as m :refer [go]]
    [redlobster.macros :refer [let-realised when-realised]])
  (:require 
    [think.object :as object]
    [cljs.core.async :refer [chan >! <! timeout]]
    [think.model :as model]
    [dommy.core :as dommy]
    [think.objects.templates.single-column :as single-column]
    [think.objects.modules.markdown :as markdown]
    [think.objects.wiki-document :as wiki-doc]
    [think.objects.workspace :as workspace]
    [think.module :as modules]
    [crate.binding :refer [subatom bound]]
    [think.util.log :refer [log log-obj]]
    [think.util.dom :as dom]))


;TODO: once finer grain pouch queries are working the templates should be loaded not hard coded
(defui new-document-form
  [this]
  [:div.offset3.span6.new-document-form
    [:label "Document Title"]
    [:input#new-document-title.span6 {:type "text" :placeholder "New document title..."}]
    [:label "Document Template"]
    [:select#new-document-template.span6 {:name "template"}
      [:option.span6 {:value "na" :selected ""} "Choose a template..."]
      [:option.span6 {:value "single-column"} "Single Column"]
      [:option.span6 {:value "two-column"} "Two Column"]]
    [:label "Create and load new document"]
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
  (object/raise think.objects.workspace/workspace :show-document new-document))


(defn create-document
  [title tpl]
  (let-realised [md-doc (markdown/markdown-doc)]
    (let-realised [tpl-doc (case tpl
                            :single-column (single-column/single-column-template-doc @md-doc))]
      (let-realised [wiki-doc (wiki-doc/wiki-doc title @tpl-doc)]
        (go
          (object/raise workspace/workspace :show-document (<! (model/load-document (:id @wiki-doc)))))))))


(dommy/listen! [(dom/$ :body) :.new-document-form :a] :click
  (fn [e]
    (.preventDefault e)
    (let [$tpl-input   (dom/$ :#new-document-template)
          tpl-type     (keyword (.-value (aget (.-options $tpl-input) (.-selectedIndex $tpl-input))))
          $title-input (dom/$ :#new-document-title)
          title        (.-value $title-input)]
      (create-document title tpl-type))))

