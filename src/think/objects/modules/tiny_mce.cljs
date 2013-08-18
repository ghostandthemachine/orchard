(ns think.objects.modules.tiny-mce
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [think.macros :refer [defui]])
  (:require [think.object    :as object]
            [crate.core      :as crate]
            [think.util.core :refer [bound-do uuid]]
            [think.util.dom  :as dom]
            [think.module    :refer [module-view spacer default-opts
                                  edit-module-btn-icon delete-btn edit-btn]]
            [think.util.log  :refer (log log-obj)]
            [crate.binding   :refer [bound subatom]]
            [think.model     :as model]
            [dommy.core      :as dommy]))


(defn tiny-mce-doc
  []
  (model/save-document
    {:type :tiny-mce-module
     :text ""
     :id   (uuid)}))


(defn render-editor
  [this]
  (let [el [:div.module-content.tiny-mce-module-content
             [:form {:method "post"}
              [:textarea.tiny-mce-editor]]]
        html (crate.core/html el)]
    html))


(def icon [:span.btn.btn-primary.tiny-mce-icon "TinyMCE"])

(defn init-tinymce
  []
  (log "Init TinyMCE")
  (.init js/tinyMCE
    (clj->js
      {:mode "specific_textareas"
       :editor_selector "tiny-mce-editor"})))


(defn ready
  [content]
  (log "Call ready")
  (log-obj content)
  (init-tinymce))


(object/object* :tiny-mce-module
                :tags #{:modules}
                :triggers #{:delete-module :save :ready}
                :behaviors [:think.module/delete-module :think.module/save-module ::ready]
                :label "TinyMCE"
                :icon icon
                :editor nil
                :ready ready
                :init (fn [this record]
                        (object/merge! this record)
                        (bound-do (subatom this :text)
                                  (fn [& args]
                                    (log "inside :text handler...")
                                    (object/raise this :save)))
                        [:div {:class (str "span12 module " (:type @this))
                               :id (str "module-" (:id @this))}
                          [:div.module-tray]
                          [:div.module-element
                            (render-editor this)]]))


(defn create-module
  []
  (go
    (let [doc (tiny-mce-doc)
          obj (object/create :tiny-mce-module doc)]
      obj)))


(dommy/listen! [(dom/$ :body) :.tiny-mce-module-content :a] :click
  (fn [e]
    ; (log "loading document: " (keyword (last (clojure.string/split (.-href (.-target e)) #"/"))))
    (think.objects.app/open-document
      (last (clojure.string/split (.-href (.-target e)) #"/")))
    (.preventDefault e)))





