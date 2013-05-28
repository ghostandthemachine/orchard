(ns think.objects.modules.html
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [redlobster.promise :as p]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.util :refer [bound-do uuid]]
            [think.objects.modules :refer [default-opts edit-module-btn-icon delete-btn edit-btn]]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))



(defn html-doc
  []
  (model/save-document
    {:type :html-module
     :text "<h3>HTML here... </h3>"
     :id   (uuid)}))

(defui render-present
  [this]
  [:div.module-content.html-module-content
    (bound (subatom this :text) #(crate/raw %))])


(defui render-edit
  []
  [:div.module-content.html-module-editor])

(def icon [:span.btn.btn-primary.html-icon "<html>"])


(defn render-module
  [this mode]
  (dom/replace-with (dom/$ (str "#module-" (:id @this) " .module-content"))
    (case mode
      :present (render-present this)
      :edit    (render-edit)))
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
  :triggers #{:save :delete}
  :behaviors [:think.objects.modules/save-module :think.objects.modules/delete-module]
  :mode :present
  :editor nil
  :init (fn [this record]
          (log "init html module")
          (log-obj this)
          (log-obj record)
          (object/merge! this record)
          (bound-do (subatom this [:mode]) (partial render-module this))
          (bound-do (subatom this :text) (fn [_] (object/raise this :save)))
          [:div.span12.module.html-module {:id (str "module-" (:id @this)) :draggable "true"}
            [:div.module-tray (delete-btn this) (edit-btn this)]
            [:div.module-element
              (render-present this)]]))


(defn create-module
  []
  (let [mod-promise (p/promise)]
    (let-realised [doc (html-doc)]
      (let [obj (object/create :html-module @doc)]
        (p/realise mod-promise obj)))
    mod-promise))



(dommy/listen! [(dom/$ :body) :.html-module-content :a] :click
  (fn [e]
    (log "loading document: " (keyword (last (clojure.string/split (.-href (.-target e)) #"/::"))))
    (think.objects.app/open-document
      (keyword
        (last (clojure.string/split (.-href (.-target e)) #"/::"))))
    (.preventDefault e)))
