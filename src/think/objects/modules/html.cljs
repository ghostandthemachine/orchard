(ns think.objects.modules.html
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.util.dom :as dom]
            [think.util :refer [bound-do]]
            [think.objects.modules :refer [default-opts edit-module-btn-icon delete-btn edit-btn]]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))


(defui render-present
  [this]
  [:div.module-content.html-module-content
    (bound (subatom this :text) #(crate/raw %))])


(defui render-edit
  [this]
  [:div.module-content.html-module-editor])


(defn render-module
  [this mode]
  (dom/replace-with (dom/$ (str "#module-" (:id @this) " .module-content"))
    (case mode
      :present (render-present this)
      :edit    (render-edit this)))
  (if (= mode :edit)
    (let [cm (js/CodeMirror
              (fn [elem]
                (dom/append (dom/$ (str "#module-" (:id @this) " .module-content"))
                  elem))
              default-opts)]
      (object/assoc! this :editor cm)
      (.setValue cm (:text @this)))
    (object/assoc! this :text (.getValue (:editor @this)))))

(object/object* :html-module
                :tags #{}
                :triggers #{:save}
                :behaviors [:think.objects.modules/save-module]
                :mode :present
                :editor nil
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this [:mode]) (partial render-module this))
                        (bound-do (subatom this :text) (fn [_] (object/raise this :save)))
                        [:div.span12.module.html-module {:id (str "module-" (:id @this)) :draggable true}
                          [:div.module-tray (delete-btn this) (edit-btn this)]
                          [:div.module-element
                            (render-present this)]]))



(dommy/listen! [(dom/$ :body) :.html-module-content :a] :click
  (fn [e]
    (log "loading document: " (keyword (last (clojure.string/split (.-href (.-target e)) #"/::"))))
    (think.objects.app/open-document
      (keyword
        (last (clojure.string/split (.-href (.-target e)) #"/::"))))
    (.preventDefault e)))
