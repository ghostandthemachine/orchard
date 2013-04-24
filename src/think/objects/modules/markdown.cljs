(ns think.objects.modules.markdown
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.util :refer [bound-do]]
            [think.util.dom :as dom]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [think.model :as model]))

(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true
     :matchBrackets true
     :viewportMargin js/Infinity}))

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
   (bound (subatom this :text) #(crate/raw (js/markdown.toHTML %)))])


(defui render-edit
  [this]
  [:div.module-content.markdown-module-editor])


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


(object/object* :markdown-module
                :tags #{}
                :triggers #{:save}
                :behaviors [:think.objects.modules/save-module]
                :mode :present
                :editor nil
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this :mode)
                                  (partial render-module this))
                        (bound-do (subatom this :text)
                                  (fn [_]
                                    (object/raise this :save)))

                        [:div.span12.module.markdown-module {:id (str "module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-element (render-present this)]]))


