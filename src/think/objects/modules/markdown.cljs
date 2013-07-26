(ns think.objects.modules.markdown
  (:require-macros
    [think.macros :refer [defui]]
    [cljs.core.async.macros :refer [go]]
    [redlobster.macros :refer [when-realised let-realised defer-node]])
  (:require
    [think.object :as object]
    [cljs.core.async :refer [chan >! <! >!! <!! put! timeout alts! close!]]
    [crate.core :as crate]
    [redlobster.promise :as p]
    [think.util.core :refer [bound-do uuid]]
    [think.util.dom :as dom]
    [think.module :refer [module-view spacer default-opts edit-module-btn-icon delete-btn edit-btn]]
    [think.util.log :refer [log log-obj]]
    [crate.binding :refer [bound subatom]]
    [think.model :as model]
    [dommy.core :as dommy]))


(defn markdown-doc
  []
  (model/save-document
    {:type :markdown-module
     :text "## Markdown module..."
     :id   (uuid)}))


(defui render-present
  [this]
  [:div.module-content.markdown-module-content
   (bound (subatom this :text) #(crate/raw (js/markdown.toHTML %)))])


(defui render-edit
  [this]
  [:div.module-content.markdown-module-editor])


(def icon [:span.btn.btn-primary.markdown-icon ".md"])


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
    (do
      (log "saving new markdown text...")
      (object/assoc! this :text (.getValue (:editor @this))))))


(object/object* :markdown-module
                :tags #{:modules}
                :triggers #{:delete-module :save}
                :behaviors [:think.module/delete-module :think.module/save-module]
                :mode :present
                :label "Markdown"
                :icon icon
                :editor nil
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this :mode)
                                  (partial render-module this))
                        (bound-do (subatom this :text)
                                  (fn [& args]
                                    (log "inside :text handler...")
                                    (object/raise this :save)))
                        (module-view this
                          [:div.module-element (render-present this)])))


(defn create-module
  []
  (let [mod-promise (p/promise)]
    (go
      (let [doc (<! (markdown-doc))
            obj (object/create :markdown-module @doc)]
        (p/realise mod-promise obj)))
    mod-promise))


(dommy/listen! [(dom/$ :body) :.markdown-module-content :a] :click
  (fn [e]
    ; (log "loading document: " (keyword (last (clojure.string/split (.-href (.-target e)) #"/"))))
    (think.objects.app/open-document
      (last (clojure.string/split (.-href (.-target e)) #"/")))
    (.preventDefault e)))






