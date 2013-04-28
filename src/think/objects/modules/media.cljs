(ns think.modules.media
  (:use [think.util :only [log]])
  (:use-macros [dommy.macros :only [sel]]
               [think.macros :only [defui]]
               [redlobster.macros :only [let-realised defer-node]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.util :refer [bound-do]]
            [think.util.dom :as dom]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [think.model :as model]
            [clojure.string :as string]
            [dommy.core :as dommy]))



(def ^:private gui     (js/require "nw.gui"))
(def ^:private os      (js/require "os"))
(def ^:private fs      (js/require "fs"))

(defn handle-file-select [handler evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (let [files (.-files (.-dataTransfer evt))]
    (handler files)
    (dotimes [i (.-length files)]
      (let [rdr (js/FileReader.)
            the-file (aget files i)]
        (log "File path: " (.-path the-file))
        ; (comment set! (.-onload rdr)
        ;       (fn [e]
        ;         (let [file-content (.-result (.-target e))
        ;               file-name (if (= ";;; " (.substr file-content 0 4))
        ;                           (let [idx (.indexOf file-content "\n\n")]
        ;                             (.log js/console idx)
        ;                             (.slice file-content 4 idx))
        ;                           (.-name the-file))]
        ;           (.log js/console (str "file-name " file-name))
        ;           (.set storage file-name file-content)
        ;           (swap! list-of-code conj file-name))))
        ; (.readAsText rdr the-file)
        ))))

(defn handle-drag-over [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (set! (.-dropEffect (.-dataTransfer evt)) "copy"))

(defn setup-media-drop-zone
  [id]
  (let [dzs (sel id)]
    (doseq [elem dzs]
      (dom/listen! elem :dragover handle-drag-over)
      (dom/listen! elem :drop     handle-file-select))))

(defn make-dropable
  [$dropzone handler]
  (dommy/listen! $dropzone :dragover handle-drag-over)
  (dommy/listen! $dropzone :drop (partial handle-file-select handler)))

(defn module-btn-icon
  [mode]
  (if (= mode :present)
    "icon-pencil module-btn"
    "icon-ok module-btn"))


(defn $media-canvas
  [this]
  (dom/$ (str "#media-canvas-" (:id @this))))


(defn $media-module-content
  [this]
  (dom/$ (str "#media-module-" (:id @this) " .media-module-content")))


(defui module-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))

(defn load-media
  [this file-path]
  (log "load media file " (:path @this))
  (let [$canvas ($media-canvas this)]
    (when (nil? (.-workerSrc js/PDFJS))
      (set! (.-workerSrc js/PDFJS) "js/media.js"))
    (.then (js/PDFJS.getDocument (:path @this))
      (fn [media]
        (log "media loaded")
        (log-obj media)
        (.then (.getPage media 1)
          (fn [page]
            (log "page loaded")
            (log-obj page)
            (let [viewport (.getViewport page 0.15)
                  context  (.getContext $canvas "2d")]
              (set! (.-height $canvas)  (.-height viewport))
              (set! (.-width $canvas)   (.-width viewport))
              (.render page
                (clj->js
                  {:canvasContext context
                   :viewport viewport})))))))))


(defn update-media
  [this file-path]
  (let [type (last (clojure.string/split file-path #"\."))]
    (log "update media for file: " type)
    ; (object/assoc! this :path file-path)
    ))


(defui load-file-button
  [handler]
  [:a.btn.btn-small.view-btn-group {:href "#load-document"} "Open"]
  :click (fn [e]
          (let [$chooser (dom/$ :#file-dialog)]
            (.preventDefault e)
            (dommy/fire! $chooser :click)
            (dommy/listen! $chooser :change
              (fn [ev]
                (handler (.-value $chooser)))))))

(defn edit-toolbar
  [this]
  [:div.row-fluid.button-bar {:id "main-toolbar-row"}
    (load-file-button (partial update-media this))])


(defui render-media
  [this]
  [:div.module-content.media-module-content
    [:canvas {:id (str "media-canvas-" (:id @this))
              :style {:border "1px solid black"}}]])

(defui render-edit
  [this]
  [:div.module-content.media-module-content
    [:div.row-fluid
      (edit-toolbar this)
      [:input#file-dialog {:style "display:none;" :type "file"}]]
    [:div.row-fluid
      [:canvas {:id (str "media-canvas-" (:id @this))
                :style {:border "1px solid black"}}]]])


(defn render-module
  [this mode]
  (dom/replace-with (dom/$ (str "#media-module-" (:id @this) " .module-content"))
    (case mode
      :present (render-media this)
      :edit    (render-edit this)))
  (case mode
    :present nil
    :edit    (make-dropable (dom/$ (str "media-canvas-" (:id @this))) #(log %))))


(object/object* :media-module
                :tags #{}
                :triggers #{:save}
                :behaviors [:think.objects.modules/save-module]
                :mode :present
                :editor nil
                :init (fn [this record]
                        (log "media path: " (:path @this))
                        (object/merge! this record)
                        (bound-do (subatom this :mode)
                          (partial render-module this))
                        (bound-do (subatom this :path)
                          (partial load-media this))
                        [:div.span12.module.media-module {:id (str "media-module-" (:id @this))}
                          [:div.module-tray (module-btn this)]
                          [:div.module-element (render-media this)]]))