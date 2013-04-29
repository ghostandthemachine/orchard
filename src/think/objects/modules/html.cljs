(ns think.objects.modules.html
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.util.dom :as dom]
            [think.util :refer [bound-do]]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))

(def default-opts
  (clj->js
    {:mode "html"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true
     :viewportMargin js/Infinity}))


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
                          [:div.module-tray (module-btn this)]
                          [:div.module-element
                            (render-present this)]]))



(dommy/listen! [(dom/$ :body) :.html-module-content :a] :click
  (fn [e]
    (log "loading document: " (keyword (last (clojure.string/split (.-href (.-target e)) #"/::"))))
    (think.objects.app/open-document
      (keyword
        (last (clojure.string/split (.-href (.-target e)) #"/::"))))
    (.preventDefault e)))
