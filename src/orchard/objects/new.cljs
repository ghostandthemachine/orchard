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
  (object/raise orchard.objects.workspace/workspace :show-document new-document))


; (defn create-document
;   [proj title]
;   (go
;     (let [mod-doc  (<! (tiny/tiny-mce-doc))
;           _ (log-obj mod-doc)
;           tpl-doc  (<! (single-column/single-column-template-doc mod-doc))
;           _ (log-obj tpl-doc)
;           wiki-doc (<! (wiki-doc/wiki-doc title tpl-doc))
;           _ (log-obj wiki-doc)
;           doc      (<! (model/load-document (:id wiki-doc)))]
;       (log-obj doc)
;       (object/raise workspace/workspace :show-document doc))))


; (dommy/listen! [(dom/$ :body) :.new-document-form :a] :click
;   (fn [e]
;     (.preventDefault e)
;     (let [$tpl-input   (dom/$ :#new-document-template)
;           tpl-type     (keyword (.-value (aget (.-options $tpl-input) (.-selectedIndex $tpl-input))))
;           $title-input (dom/$ :#new-document-title)
;           title        (.-value $title-input)]
;       (create-document title tpl-type))))
