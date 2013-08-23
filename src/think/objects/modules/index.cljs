(ns think.objects.modules.index
  (:require-macros
    [think.macros :refer [defui]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async    :refer [<!]]
    [think.object       :as object]
    [crate.core         :as crate]
    [think.util.dom     :as dom]
    [think.model        :as model]
    [think.util.core    :refer [bound-do]]
    [think.util.log     :refer (log log-obj)]
    [crate.binding      :refer [bound subatom]]
    [dommy.core         :as dommy]))


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
    [:ul
      (for [doc docs]
        [:li
          [:a {:href (:id doc)} (str (when (:project doc) (str (:project doc) " - ")) (:title doc))]])]])


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
    (let [docs (<! (model/all-wiki-documents))]
      (dom/replace-with ($module this) (render-present docs)))))


(defn render-module
  [this mode]
  (case mode
    :present (load-index this)
    :edit (dom/replace-with ($module this) (render-edit this))))


(object/object* :index-module
                :tags #{:module}
                :triggers #{:save}
                :behaviors [:think.objects.modules/save-module]
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        (log "creating index module")
                        (log-obj (clj->js @this))
                        (bound-do (subatom this [:mode]) (partial render-module this))
                        (load-index this)
                        [:div.span12.module.index-module {:id (str "module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-content.index-module-content]]))


(dommy/listen! [(dom/$ :body) :.index-module-content :a] :click
  (fn [e]
    (think.objects.app/open-document (last (clojure.string/split (.-href (.-target e)) #"/")))
    (.preventDefault e)))
