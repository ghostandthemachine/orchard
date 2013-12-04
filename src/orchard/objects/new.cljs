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
    [orchard.model :as model]
    [crate.core :as crate]
    [crate.binding :refer [subatom bound]]
    [orchard.util.log :refer (log log-obj)]
    [orchard.util.dom :as dom]))


(defn create-new-document
  [db {:keys [title project-id] :as data}]
  (log "create-new-document")
  (log-obj data)
  (let [editor    (editor/editor-doc db)
        tpl-doc   (single-column/single-column-template-doc db editor)
        wiki-page (wiki-page/wiki-page db {:title title :template tpl-doc :project project-id})]
    (go
      (let [proj      (<! (model/get-object orchard.objects.app/db project-id))
            ;; add the new wiki page to the projects document vector
            proj      (update-in proj [:documents]
                    (fn [docs]
                      (conj docs (:id wiki-page))))]
        (model/save-object! db (:id proj) proj)))
    (doseq [obj [editor tpl-doc wiki-page]]
      (model/save-object! db (:id obj) obj))
    wiki-page))


(defn project-select
  [projects]
  [:select#project-titles.form-control
    [:optgroup  
      (for [p projects]
        [:option {:value (:id p)} (:title p)])
      [:option {:disabled "disabled"} "---------------"]]
    [:optgroup
      [:option {:value "new-project"} "New Project..."]]])


(defn project-title-input
  []
  [:input#new-project-title.form-control {:type "text" :placeholder "New Project"}])


(defui create-button
  []
  [:button#create-doc-btn.btn.btn-default "Create Document"]
  :click (fn [e]
           (.preventDefault e)
           (let [$title-input   (dom/$ :#new-document-title)
                 title          (.-value $title-input)
                 $project-input (dom/$ :#project-titles)
                 project-id     (.-value $project-input)
                 $editor-input  (.getElementById js/document "editor-check")
                 new-page       (create-new-document orchard.objects.app.db {:title title
                                                                             :project project-id})])))


(defui create-project-button
  []
  [:button#create-project-btn.btn.btn-default "Create Project"]
  :click  (fn [e]
            (.preventDefault e)
            (let [$project-input (dom/$ :#new-project-title)
                  project-title  (.-value $project-input)]
              (go
                (let [new-project (model/create-project orchard.objects.app/db {:title project-title})
                      projects    (<! (model/all-projects orchard.objects.app/db))]
                  (dom/replace-with (dom/$ :#new-project-title) (crate/html (project-title-input)))
                  (dom/replace-with (dom/$ :#project-titles) (crate/html (project-select projects)))
                  ;; set select value to new project
                  (aset (dom/$ :#project-titles) "value" (:id new-project)))))))


;TODO: once finer grain pouch queries are working the templates should be loaded not hard coded
(defui new-document-form
  [this projects]
  [:div.col-md-6
    [:form.new-document-form
      [:div.form-group
        [:label {:for "project-titles"} "Select Project"]
        (project-select projects)]
      [:div.form-group
        [:label {:for "new-project-title"} "Project Title"]
        (project-title-input)]
      (create-project-button)
      [:br]
      [:br]
      [:div.form-group
        [:label {:for "new-document-title"} "Document Title"]
        [:input#new-document-title.form-control {:type "text" :placeholder "New Document"}]]
      (create-button)]])


(object/object* :new-document
  :triggers #{}
  :behaviors []
  :init (fn [this projects]
          [:div.container.new-document-container
            [:div.span9.offset3
              [:div.row
                [:h3 "New Wiki Document"]
                [:br]
                [:br]]
              [:div.row
                (new-document-form this projects)]]]))


(defn load-new-doc
  []
  (go
    (let [projects (<! (model/all-projects orchard.objects.app/db))]
      (object/raise orchard.objects.workspace/workspace
        :show-page (object/create :new-document projects)))))