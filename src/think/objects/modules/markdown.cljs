(ns think.objects.modules.markdown
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.util.dom :as dom]
            [think.util.log :refer [log]]
            [crate.binding :refer [bound subatom]]))

(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true}))

(defui module-btn
  [this]
  [:div.module-btn]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))

(defui render-present
  [this]
  [:div.module-content.markdown-module-content
    (crate/raw (js/markdown.toHTML (:text @this)))])


(defui render-edit
  [this]
  [:div.module-content.markdown-module-editor])



(defn render-module
  [this mode]
  (dom/replace-with (dom/$ (str "#module-" (:id @this) " .module-content"))
    (case mode
      :present (render-present this)
      :edit    (render-edit this)))
  (when (= mode :edit)
    (let [cm (js/CodeMirror
              (fn [elem]
                (dom/append (dom/$ (str "#module-" (:id @this) " .module-content"))
                  elem))
              (clj->js default-opts))])))

(defn bound-do
  [a* handler]
  (add-watch a* :mode-toggle-watch
    (fn [k elem* ov nv]
      (handler nv))))


(object/object* :markdown-module
                :tags #{}
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this [:mode]) (partial render-module this))
                        [:div.module.markdown-module {:id (str "module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-element
                            (render-present this)]]))
