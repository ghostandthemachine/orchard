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

(defui render-present
  [docs]
  [:div.module-content.index-module-content
   [:h3 "Index"]
    [:div.span4
     [:ul
      (for [doc docs]
        [:li
          [:a {:href (:id doc)} (str (when (:project doc) (str (:project doc) " - ")) (:title doc))]])]]
   [:div.span4
    [:canvas#tree-canvas {:width 400 :height 600}]]])


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
    (let [docs (<! (model/all-wiki-pages orchard.objects.app.db))]
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
                        [:div.container-fluid
                          [:div.span11.module.index-module {:id (str "module-" (:id @this))}
                            [:div.module-tray (module-btn this)]
                            [:div.module-content.index-module-content]]]))


(dommy/listen! [(dom/$ :body) :.index-module-content :a] :click
  (fn [e]
    (orchard.objects.app/open-page orchard.objects.app.db (last (clojure.string/split (.-href (.-target e)) #"/")))
    (.preventDefault e)))
