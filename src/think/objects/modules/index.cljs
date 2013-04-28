(ns think.objects.modules.index
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [redlobster.promise :as p]
            [think.util.dom :as dom]
            [think.util :refer [bound-do]]
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
    (for [doc docs]
      [:a {:href (:id doc)} (str (:id doc) " (" (:type doc) ")")])])


(defui render-edit
  [this]
  [:div.module-content.index-module-editor
   "Indexes don't have any settings at the moment..."])


(defn render-module
  [this mode]
  (let [tgt (dom/$ (str "#module-" (:id @this) " .module-content"))]
    (case mode
      :present (p/let-realised [docs (model/all-documents)]
                 (dom/replace-with tgt (render-present @docs)))
      :edit (dom/replace-with tgt (render-edit this)))))

(object/object* :index-module
                :tags #{}
                :triggers #{:save}
                :behaviors [:think.objects.modules/save-module]
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this [:mode]) (partial render-module this))
                        [:div.span12.module.-module {:id (str "module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-element]]))


(dommy/listen! [(dom/$ :body) :.index-module-content :a] :click
  (fn [e]
    (log "loading document: " (.-href (.-target e)))
    (think.objects.app/open-document (.-href (.-target e)))
    (.preventDefault e)))
