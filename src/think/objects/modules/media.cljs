(ns think.objects.modules.media
  (:use [think.util :only [log log-obj uuid]])
  (:use-macros [dommy.macros :only [sel]]
               [think.macros :only [defui]]
               [redlobster.macros :only [let-realised defer-node]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.objects.modules :refer [module-btn-icon module-btn]]
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


(defn prepare-media-canvas
  [this]
  (let [$content (dom/$ (str "#media-content-" (:id @this)))
        canvas   (.createElement js/document "canvas")]
  (dom/empty $content)
  (aset canvas "id" (str "#media-canvas-" (:id @this)))
  (dom/append $content canvas)))



(defn $media-module-content
  [this]
  (dom/$ (str "#media-module-" (:id @this) " .media-module-content")))




(defn load-pdf
  [this file-path]
  (log "load media file " (:path @this))
  (prepare-media-canvas this)
  (let [canvas (dom/$ (str "#media-canvas-" (:id @this)))]
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
                  context  (.getContext canvas "2d")]
              (set! (.-height canvas)  (.-height viewport))
              (set! (.-width canvas)   (.-width viewport))
              (.render page
                (clj->js
                  {:canvasContext context
                   :viewport viewport})))))))))


(defn update-path   [this file-path]
  (let [type (keyword (last (clojure.string/split file-path #"\.")))]
    (log "update media for file: " type)
    (object/assoc! this
      :path file-path
      :type (case type
              (:jpg :jpeg :tif :tiff :png :gif)
                :img
              (:MP4 :mp4 :WebM :webm :Ogg :ogg)
                :video
              :pdf
                :pdf
              (log "Unsupported file type: " type)))))




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
    (load-file-button (partial update-path this))])


;; Media renderers

(defn render-img
  [path]
  [:img.media-content {:src path}])


(defn render-video
  [path]
  [:video.media-content {:src path}])


(defn render-pdf
  [path]
  [:canvas.media-content])


(defn render-place-holder
  []
  [:div.media-place-holder])


(defui render-media
  [this]
  [:div.module-content.media-module-content
    (let [type (:type @this)]
      (case type
        :img   (render-img   (:path @this))
        :video (render-video (:path @this))
        :pdf   (render-pdf   (:path @this))
        (render-place-holder)))])



(defui render-edit
  [this]
  [:div.module-content.media-module-content
    [:div.row-fluid
      (edit-toolbar this)
      [:input#file-dialog {:style "display:none;" :type "file"}]]
    [:div.row-fluid
      [:canvas {:id (str "media-content-" (:id @this))
                :style {:border "1px solid black"}}]]])


(defn render-module
  [this mode]
  (dom/replace-with (dom/$ (str "#media-module-" (:id @this) " .module-content"))
    (case mode
      :present (render-media this)
      :edit    (render-edit this)))
  (case mode
    :present nil
    :edit    (make-dropable (dom/$ (str "media-content-" (:id @this))) #(log %))))


(def icon [:span.btn.btn-primary.html-icon "media"])

; (object/object* :media-module
;                 :tags #{}
;                 :triggers #{:save :delete}
;                 :behaviors [:think.objects.modules/save-module :think.objects.modules/delete-module]
;                 :mode :present
;                 :editor nil
;                 :type nil
;                 :init (fn [this record]
;                         (log "init media module")
;                         (log-obj this)
;                         (log-obj record)
;                         (object/merge! this record)
;                         (bound-do (subatom this :mode)
;                           (partial render-module this))
;                         (bound-do (subatom this :path)
;                           (partial update-path this))
;                         [:div.span12.module.media-module {:id (str "media-module-" (:id @this))}
;                           [:div.module-tray (module-btn this)]
;                           [:div.module-element (render-media this)]]))


; (defn create-module
;   [doc]
;   (object/create :media-module doc))



; (defn media-doc
;   []
;   {:type :media-module
;    :path ""
;    :id   (uuid)})



(defn media-doc
  []
  {:type :media-module
   :path nil
   :id   (uuid)})


(object/object* :media-module
  :tags #{}
  :triggers #{:save :delete}
  :behaviors [:think.objects.modules/save-module :think.objects.modules/delete-module]
  :mode :present
  :editor nil
  :init (fn [this record]
          (log "init media module")
          (log-obj this)
          (log-obj record)
          (object/merge! this record)
          (bound-do (subatom this [:mode]) (partial render-module this))
          (bound-do (subatom this :text) (fn [_] (object/raise this :save)))
          [:div.span12.module.media-module {:id (str "module-" (:id @this)) :draggable "true"}
            [:div.module-tray (delete-btn this) (edit-btn this)]
            [:div.module-element
              [:h1 "wkejgbnkwg"]]]))


(defn create-module
  [doc]
  (object/create :media-module doc))