(ns orchard.objects.modules.editor
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [orchard.macros :refer [defui]])
  (:require [cljs.core.async    :refer [chan >! <!]]
            [orchard.util.module     :refer (module-view spacer default-opts
                                             edit-module-btn-icon delete-btn
                                             edit-btn handle-delete-module)]
            [orchard.util.core  :refer (bound-do uuid)]
            [orchard.util.log   :refer (log log-obj)]
            [crate.binding      :refer (bound subatom)]
            [orchard.object     :as object]
            [crate.core         :as crate]
            [orchard.model      :as model]
            [orchard.objects.editable.editor :as editor]))


(defn editor-doc
  [db]
  (let [id (uuid)]
    (model/save-object! db id
      {:type :editor-module
      :text ""
      :id   id})))


(defn sel [this] (str "editor-" (:id @this)))


(defn render-editor
  [this]
  (crate/html
    [:div {:id (sel this)}
      (:text @this)]))


(def icon [:span.btn.btn-primary.editor-icon "editor"])


(defn init-editor
  [this]
  (log "call init editor")
  (let [ed-obj (editor/editor (sel this))
        editor (:editor ed-obj)]
    (object/assoc! this :editor editor)
    (.blur editor
    (fn [event arg]
      (let [content (.getContents editor)]
        (log "saving editor content: " content)
        (object/assoc! this :text content))))))


(object/object* :editor-module
                :tags #{:modules}
                :triggers #{:delete-module :save :ready}
                :behaviors [:orchard.util.module/delete-module :orchard.util.module/save-module]
                :label "editor"
                :icon icon
                :ready init-editor
                :text ""
                :init (fn [this record]
                        (log "new editor module: ")
                        (log-obj (clj->js record))
                        (object/merge! this record)
                        [:div {:class (str "span12 module " (:type @this))
                               :id (str "module-" (:id @this))}
                          (render-editor this)]))


(defn create-module
  [app]
  (go
    (object/create :editor-module
      (<! (editor-doc (:db app))))))