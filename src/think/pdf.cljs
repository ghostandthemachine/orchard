(ns think.pdf
  (:use [think.util :only [log]])
  (:use-macros [dommy.macros :only [sel]])
  (:require [think.view-helpers :as view]
            [dommy.core :as dom]))

(def ^:private gui     (js/require "nw.gui"))
(def ^:private os      (js/require "os"))
(def ^:private fs      (js/require "fs"))

(defn file-drop
  [elem handler]
  (set! (.-ondragover js/window) (fn [e] (.preventDefault e) false))
  (set! (.-ondrop js/window) (fn [e] (.preventDefault e) false))

  (-> (jq/$ elem)
      (.ondragover (fn []
                     (this-as spot
                              (set! (.-className spot) "hover"))
                     false))

      (.ondragend (fn []
                    (this-as spot
                             (set! (.-className spot) ""))))

      (.ondrop (fn [e]
                 (.preventDefault e)
                 (println "drop testing...")
                 (doseq [file (.-files (.-dataTransfer e))]
                   (log "File path: " (.-path file))
                   (js/alert "path: " (.-path file)))
                 ; (try
                 ;   (.Shell.showItemInFolder gui (.-path file))
                 ;   (catch js/Error e
                 ;     (log "Got an error: " e))
                 ;   (catch js/global.Error e
                 ;     (log "Got a global error: " e)))
                 false))

      ))


(defn handle-file-select [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (let [files (.-files (.-dataTransfer evt))]
    (dotimes [i (.-length files)]
      (let [rdr (js/FileReader.)
            the-file (aget files i)]
        (log "File path: " (.-path the-file))
        (comment set! (.-onload rdr)
              (fn [e]
                (let [file-content (.-result (.-target e))
                      file-name (if (= ";;; " (.substr file-content 0 4))
                                  (let [idx (.indexOf file-content "\n\n")]
                                    (.log js/console idx)
                                    (.slice file-content 4 idx))
                                  (.-name the-file))]
                  (.log js/console (str "file-name " file-name))
                  (.set storage file-name file-content)
                  (swap! list-of-code conj file-name))))
        (.readAsText rdr the-file)))))

(defn handle-drag-over [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (set! (.-dropEffect (.-dataTransfer evt)) "copy"))

(defn setup-pdf-drop-zone
  [id]
  (let [dzs (sel id)]
    (doseq [elem dzs]
      (dom/listen! elem :dragover handle-drag-over)
      (dom/listen! elem :drop     handle-file-select))))


(defn workspace-sidebar
  []
  [:div.workspace-sidebar {:id "workspace-sidebar"}
    (for [i (range 10)]
      [:div.row
        [:div.pdf-thumbnail]])])

(comment
  (think.core/r!)
  )

(defn main-pdf-viewer
  []
  [:div#pdf-element
    (workspace-sidebar)
    [:div.workspace-content {:id "workspace-content"}
      [:canvas {:id "main-pdf-viewer"
                :style {:border "1px solid black"}}]]])

(defn page-load-handler
  ([page id]
    (page-load-handler page id 1.25))
  ([page id scale]
  (log "page-load-handler")
  (let [viewport (.getViewport page scale)
        canvas   (first (sel id))
        context  (.getContext canvas "2d")]
    (set! (.-height canvas) (.-height viewport))
    (set! (.-width canvas) (.-width viewport))
    (.render page
      {"cavasContext" context
       "viewport" viewport}))))

(defn pdf-load-handler
  [pdf id load-handler page-num]
  "Fetch a using promise."
  (log "pdf-load-handler")
  (let [promise (.getPage pdf page-num)]
    (.then (partial page-load-handler id))))

(def pdfs* (atom {}))

(def default-pdf-opts
  {:pdf-load-handler  pdf-load-handler
   :page-load-handler page-load-handler})

(defn load-page
  "Load a page in a pdf instance if it exists. Optionally takes a map of PDF and page load handler as well as the page number."
  [id page-number & opts]
  (if (some #{id} (keys @pdfs*))
    (let [pdf-opts          (merge default-pdf-opts opts)
         {pdf-load-handler  :pdf-load-handler
          page-load-handler :page-load-handler} pdf-opts
          pdf               (id @pdfs*)]
      (.then pdf
        (partial pdf-load-handler page-load-handler id page-number))))
    (log "That PDF does not exist. ID: " id))

(defn create-pdf
  "Creates a new pdf instance and adds the instance to the pdfs* map using the id as a key. Returns the pdf instance."
  [id url]
  (->
    (swap! pdfs* assoc id
      (js/PDFJS.getDocument url))
    id))


(comment

(let [canvas   (first (sel "#main-pdf-viewer"))]
  (.then (js/PDFJS.getDocument "test.pdf")
    (fn [pdf]
      (.then (.getPage pdf 2)
        (fn [page]
          (let [viewport (.getViewport page 1.25)
                context  (.getContext canvas "2d")]
            (set! (.-height canvas) (.-height viewport))
            (set! (.-width canvas) (.-width viewport))
            (let [promise (.render page
                            {:cavasContext context
                             :viewport viewport})]
              promise)))))
    #(log "pdf load error")
    #(log "pdf load progress " (js->clj %))))

  (js/LoadPDF "test.pdf" (first (sel "#main-pdf-viewer")) 3 1.25)

  (think.core/r!)
)