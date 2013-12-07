(ns orchard.objects.modules.editor
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [orchard.macros :refer [defui]])
  (:require [cljs.core.async          :refer [chan >! <! put!]]
            [orchard.util.module      :refer (module-view spacer edit-module-btn-icon delete-btn edit-btn handle-delete-module)]
            [orchard.util.core        :refer (bound-do uuid)]
            [orchard.util.log         :refer (log log-obj)]
            [orchard.util.dom         :as dom]
            [orchard.editable.toolbar :as toolbar]
            [orchard.editable.core    :refer [event-chan make-editable! selected-node has-node]]
            [orchard.object           :as object]
            [orchard.observe          :as observe]
            [orchard.model            :as model]
            [dommy.core               :as dommy]
            [crate.binding            :refer (bound subatom)]
            [crate.core               :as crate]))


(defn sel [this] (str "editor-" (:id @this)))


(defn default-editor-element
  [opts]
  [:div.editable])


(def icon [:span.btn.btn-primary.editor-icon "editor"])



(defn set-btn-active
  [editor sel b]
  (let [btn     (dom/$ (:toolbar editor) sel)
        active? (dom/has-class? btn "active")]
    (if b
      (when-not active?
        (dom/add-class btn "active"))
      (when active?
        (dom/remove-class btn "active")))))


(defn toggle-bold-btn
  [editor]
  (if (= (.-nodeName (selected-node)) "B")
    (set-btn-active editor "bold-btn" true)
    (set-btn-active editor "bold-btn" false)))


;; Channel based go block handlers
(defn mouse-down-handler
  [{:keys [channels] :as editor}]
  (go
    (loop []
      (let [ev (<! (:mouse-down channels))]
        (log-obj (selected-node))
        (toggle-bold-btn editor))
      (recur))))

(defn mouse-up-handler
  [{:keys [channels] :as editor}]
  (go
    (loop []
      (let [ev (<! (:mouse-up channels))]
        (log-obj (selected-node))
        (toggle-bold-btn editor)
      (recur)))))


(defn input-handler
  [{:keys [channels] :as editor}]
  (go
    (while true
      (let [in (<! (:input channels))]
        ; (log-obj in)
        ))))


(def default-opts
  {:editor-height "100px"})


(defn create-editor
  [this & opts]
  (let [opts              (merge default-opts (apply hash-map (flatten (partition 2 opts))))

        element           (crate/html (default-editor-element opts))
        toolbar           (crate/html (toolbar/view))

        mouse-click-chan  (event-chan element :click)
        mouse-down-chan   (event-chan element :mousedown)
        mouse-up-chan     (event-chan element :mouseup)
        blur-chan         (event-chan element :blur)
        focus-chan        (event-chan element :focus)
        input-chan        (event-chan element :input)

        editor            (merge opts
                            {:container   [:div.editor-container toolbar element]
                             :element     element
                             :toolbar     toolbar
                             :channels   {:blur         blur-chan
                                          :focus        focus-chan
                                          :input        input-chan
                                          :mouse-click  mouse-click-chan
                                          :mouse-down   mouse-down-chan
                                          :mouse-up     mouse-up-chan}})]
        ;; look and feel
    (dom/set-css element {:height (:editor-height opts)})
    (make-editable! element)
    (input-handler editor)
    editor))

(defn toolbar
  [this]
  (get-in @this [:editor :toolbar]))


(defn editor-element
  [this]
  (get-in @this [:editor :element]))


(defn editor-container
  [this]
  (get-in @this [:editor :container]))


(defn editor-content
  [this]
  (aget (editor-element this) "innerHTML"))

(defn set-editor-content
  [this content]
  (aset (editor-element this) "innerHTML" content))


(defn initialize-editor
  "Here we intialize editor content and set selection ids. Doing this here
  allows editr creatino to be independant of the object system."
  [this]
  (aset (editor-element this) "id" (sel this))
  (set-editor-content this (:text @this))
  ;; observe inputs changes and save
  (go
    (while true
      (let [e (<! (get-in @this [:editor :channels :input]))]
        (object/assoc! this
          :text (editor-content this)))))
  ;; return the container element holding the editor and toolbar
  (editor-container this))


(object/object* :editor-module
                :tags #{:modules}
                :triggers #{:delete-module :save :ready}
                :behaviors [:orchard.util.module/delete-module :orchard.util.module/save-module]
                :label "editor"
                :icon icon
                :text ""
                :init (fn [this record]
                        (object/merge!
                          this
                          record
                          {:editor (create-editor this)})

                        (bound-do (subatom this :text)
                          (fn [& args]
                            (object/raise this :save)))

                        (initialize-editor this)

                        [:div {:class (str "span12 module " (:type @this))
                               :id (str "module-" (:id @this))}
                          (editor-container this)]))


(defn editor-doc
  [db]
  (let [id (uuid)]
    (model/save-object! db id
      {:type :editor-module
      :text ""
      :id   id})))

(defn create-module
  [app]
  (go
    (object/create :editor-module
      (<! (editor-doc (:db app))))))



(dommy/listen! [(dom/$ :body) :.editor-container :a] :click
  (fn [e]
    (go
      (let [page-link (last (clojure.string/split (.-href (.-target e)) #"/"))]
        (orchard.objects.app/open-page orchard.objects.app/db page-link)))
    (.preventDefault e)))