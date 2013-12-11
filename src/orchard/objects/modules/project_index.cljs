(ns orchard.objects.modules.project-index
  (:refer-clojure :exclude [defonce])
  (:require-macros
    [orchard.macros         :refer [defui defonce]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [orchard.object        :as object]
    [crate.core            :as crate]
    [orchard.util.dom      :as dom]
    [orchard.model         :as model]
    [orchard.graphics.tree :as tree]
    [dommy.core            :as dommy]
    [cljs.core.async       :refer (<! timeout)]
    [orchard.util.core     :refer (bound-do)]
    [orchard.util.log      :refer (log log-obj)]
    [crate.binding         :refer (bound subatom)]))




(defn icon [n]
  [:span {:class (str "glyphicon " n)}])


(defn create-new-wiki-page
  [db {:keys [title] :as data}]
  (let [editor    (model/editor-module)
        tpl-doc   (model/single-column-template (:id editor))
        wiki-page (model/wiki-page {:title title :template (:id tpl-doc)})]
    (doseq [obj [editor tpl-doc wiki-page]]
      (model/save-object! db (:id obj) obj))
    wiki-page))



(defui delete-page-btn
  [page]
  [:td.icon-cell (icon "glyphicon-trash")]
  :click  (fn [e]
            (model/delete-page orchard.objects.app/db (:id page))
            (orchard.objects.app/show-project orchard.objects.app/db (orchard.objects.workspace/current-project))))


(defui create-document-btn
  []
  [:span.input-group-addon "new"]
  :click (fn [e]
           (go
             (let [$title-input   (dom/$ :#new-document-title)
                   title          (.-value $title-input)
                   project-id     (orchard.objects.workspace/current-project)
                   $editor-input  (.getElementById js/document "editor-check")
                   new-page       (create-new-wiki-page orchard.objects.app/db {:title title :project project-id})]
              ;; associate project and new page
              (model/add-document-to-project orchard.objects.app/db project-id (:id new-page))
              ;; update nav
              (orchard.sidebar/update-sidebar)
              ;; show new page
              (orchard.objects.app/open-page orchard.objects.app/db (:id new-page))))
            (.preventDefault e)))



(defui render-present
  [proj]
  [:div.module-content.index-module-content
    [:div.row
      [:h3
        (icon "glyphicon-folder-open")
        "     "
        (:title proj)]]
    [:div.row
      [:table.table.table-striped
        [:tbody
          (for [doc (:documents proj)]
            (let [_ (log (:title doc))
                  lbl (:title doc)]
              [:tr
                [:td.icon-cell (icon "glyphicon-file")]
                [:td
                  [:a {:href (:id doc)} lbl]]
                (delete-page-btn doc)]))]]]
    [:div.row
      [:div.input-group
        (create-document-btn)
        [:input#new-document-title.form-control {:type "text" :placeholder "document name..."}]]]])


(defui render-edit
  [this]
  [:div.module-content.index-module-editor
   "Indexes don't have any settings at the moment..."])


(defn $module
  [this]
  (dom/$ (str "#module-" (:id @this) " .module-content")))


(defn load-index
  [this]
  (go
    (let [db orchard.objects.app/db
          id (:project @this)
          proj  (<! (model/get-object-with-dependants db id :documents))]
      (log "load-index id: " id)
      (log-obj (:documents proj))
      (dom/replace-with ($module this) (render-present proj)))))


(object/object* :project-index-module
                :tags #{:module}
                :triggers #{:save}
                :behaviors [:orchard.objects.modules/save-module]
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        (load-index this)
                        [:div.container
                          [:div.span11.module.index-module {:id (str "module-" (:id @this))}
                            [:div.module-content.project-index-module-content]]]))


(dommy/listen! [(dom/$ :body) :.index-module-content :a] :click
  (fn [e]
    (orchard.objects.app/open-page orchard.objects.app.db (last (clojure.string/split (.-href (.-target e)) #"/")))
    (.preventDefault e)))