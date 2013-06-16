(ns think.objects.modules.index
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [redlobster.promise :as p]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.util.core :refer [bound-do]]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))


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
    [:ul
      (for [doc docs]
        [:li
          [:a {:href (:id doc)} (:title doc)]])]])


(defui render-edit
  [this]
  [:div.module-content.index-module-editor
   "Indexes don't have any settings at the moment..."])


(defn $module
  [this]
  (dom/$ (str "#module-" (:id @this) " .module-content")))


(defn load-index
  [this]
  (let-realised [docs (model/all-wiki-documents)]
    (dom/replace-with ($module this) (render-present @docs))))


(defn render-module
  [this mode]
  (case mode
    :present (load-index this)
    :edit (dom/replace-with ($module this) (render-edit this))))


(object/object* :index-module
                :tags #{}
                :triggers #{:save}
                :behaviors [:think.objects.modules/save-module]
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this [:mode]) (partial render-module this))
                        (load-index this)
                        [:div.span12.module.index-module {:id (str "module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-content.index-module-content]]))


(dommy/listen! [(dom/$ :body) :.index-module-content :a] :click
  (fn [e]
    (log "loading document: " (.-href (.-target e)))
    (think.objects.app/open-document (last (clojure.string/split (.-href (.-target e)) #"/")))
    (.preventDefault e)))
