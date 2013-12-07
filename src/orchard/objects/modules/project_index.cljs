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


(defui render-present
  [proj docs]
  [:div.module-content.index-module-content
    [:div.row
      [:h3 "Index"]]
    [:div.row
      [:div.span4
       [:ul
        (for [doc docs]
          (let [lbl (str (:title proj) "/" (:title doc))]
            [:li
              [:a {:href (:id doc)} lbl]]))]]
      [:div.span4
        [:canvas#tree-canvas {:width 400 :height 400}]]]])


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
    (let [proj  (<! (model/get-object orchard.objects.app/db (:project @this)))
          docs* (atom [])]
      (doseq [id (:documents proj)]
        (swap! docs* conj (<! (model/get-object orchard.objects.app/db id))))
      (dom/replace-with ($module this) (render-present proj @docs*)))))


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
