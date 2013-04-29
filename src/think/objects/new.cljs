(ns think.objects.new
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised when-realised]])
  (:require [think.object :as object]
            [think.model :as model]
            [think.util.dom :as dom]
            [dommy.core :as dommy]
            [think.util :as util]
            [think.objects.templates.single-column :as single-column]
            [think.objects.modules :as modules]
            [crate.binding :refer [subatom bound]]
            [think.util.log :refer [log log-obj]]))


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



(dommy/listen! [(dom/$ :body) :.new-document-form :a] :click
  (fn [e]
    (.preventDefault e)
    (let [$tpl-input   (dom/$ :#new-document-template)
          tpl-type     (str (.-value (aget (.-options $tpl-input) (.-selectedIndex $tpl-input))) "-template")
          $title-input (dom/$ :#new-document-title)
          title        (.-value $title-input)
          id           (str (util/uuid) "-" (clojure.string/replace title #" " "-"))
          tpl          {:type tpl-type
                        :id (util/uuid)
                        :modules []}
          ndoc         {:type :wiki-document
                        :id id
                        :title title
                        :template (:id tpl)}]
      (let-realised [tpl-doc (model/save-document tpl)]
        (let-realised [new-doc (model/save-document ndoc)]
          (think.objects.app/open-document (:id ndoc)))))))