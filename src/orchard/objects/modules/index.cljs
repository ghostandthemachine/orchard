(ns orchard.objects.modules.index
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


(defn module-btn-icon
  [mode]
  (if (= mode :present)
    "icon-pencil module-btn"
    "icon-ok module-btn"))


(defui module-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))

; (defui render-present
;   [docs]
;   [:div.module-content.index-module-content
;     [:div.row
;       [:h3 "Projects"]]
;     [:div.row
;       [:div.span4
;        [:ul
;         (for [d docs]
;           [:li
;             [:a {:href (:root d)} (:title d)]])]]
;       [:div.span4
;         [:canvas#tree-canvas {:width 400 :height 200}]]]])


(defui delete-project-btn
  [project]
  [:td.icon-cell (icon "glyphicon-trash")]
  :click  (fn [e]
            (model/delete-project orchard.objects.app/db (:id project))
            (orchard.objects.app/show-project orchard.objects.app/db :home)))


(defn create-new-wiki-page
  [db {:keys [title] :as data}]
  (let [editor    (model/editor-module)
        tpl-doc   (model/single-column-template (:id editor))
        wiki-page (model/wiki-page {:title title :template (:id tpl-doc)})]
    (doseq [obj [editor tpl-doc wiki-page]]
      (model/save-object! db (:id obj) obj))
    wiki-page))


(defui create-document-btn
  []
  [:span.input-group-addon "new"]
  :click (fn [e]
           (let [$project-input (dom/$ :#new-project-title)
                  project-title  (.-value $project-input)]
              (go
                (let [new-project (model/create-project orchard.objects.app/db {:title project-title})]
                  ;; update nav
                  (orchard.sidebar/update-sidebar)
                  (orchard.objects.app/show-project orchard.objects.app/db (:id new-project)))))))



(defui render-present
  [projects]
  [:div.module-content.index-module-content
    [:div.row
      [:h3 "Projects"]]
    [:div.row
      [:table.table.table-striped
        [:tbody
          (for [project (filter #(not= (:title %) "Home") projects)]
            (let [lbl (or (:title project) "root")]
              [:tr
                [:td.icon-cell (icon "glyphicon-folder-close")]
                [:td
                  [:a {:href (:root project)} lbl]]
                (delete-project-btn project)]))]]]
    [:div.row
      [:div.input-group
        (create-document-btn)
        [:input#new-project-title.form-control {:type "text" :placeholder "project name..."}]]]])


(defui render-edit
  [this]
  [:div.module-content.index-module-editor
   "Indexes don't have any settings at the moment..."])


(defn $module
  [this]
  (dom/$ (str "#module-" (:id @this) " .module-content")))


(defonce load-tree? (atom true))

(defn load-index
  [this]
  (go
    (let [docs (<! (model/all-projects orchard.objects.app/db))]
      (dom/replace-with ($module this) (render-present docs))
      (when @load-tree? 
        (reset! load-tree? false)
        (tree/draw-tree "tree-canvas")))))


(defn render-module
  [this mode]
  (case mode
    :present (load-index this)
    :edit (dom/replace-with ($module this) (render-edit this))))


(object/object* :index-module
                :tags #{:module}
                :triggers #{:save}
                :behaviors [:orchard.objects.modules/save-module]
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this [:mode]) (partial render-module this))
                        (load-index this)
                        [:div.container
                          [:div.span11.module.index-module {:id (str "module-" (:id @this))}
                            [:div.module-content.index-module-content]]]))


(dommy/listen! [(dom/$ :body) :.index-module-content :a] :click
  (fn [e]
    (go
      (let [project-link (last (clojure.string/split (.-href (.-target e)) #"/"))]
        (orchard.objects.app/open-page orchard.objects.app/db project-link)))
    (.preventDefault e)))