(ns orchard.objects.media.pdf
  (:require-macros
    [dommy.macros :refer [sel]]
    [orchard.macros :refer [defui]])
  (:require [orchard.object :as object]
            [crate.core :as crate]
            [orchard.util.core :refer [bound-do]]
            [orchard.util.dom :as dom]
            [orchard.util.log :refer (log log-obj)]
            [crate.binding :refer [bound subatom]]
            [orchard.model :as model]
            [dommy.core :as dommy]))


(defn $pdf-canvas
  [this]
  (dom/$ (str "#pdf-canvas-" (:id @this))))


(defn load-pdf
  [this file-path]
  (log "load pdf file " (:path @this))
  (let [$canvas ($pdf-canvas this)]
    (when (nil? (.-workerSrc js/PDFJS))
      (set! (.-workerSrc js/PDFJS) "js/pdf.js"))
    (.then (js/PDFJS.getDocument (:path @this))
      (fn [pdf]
        (log "pdf loaded")
        (log-obj pdf)
        (.then (.getPage pdf 1)
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


(object/behavior* ::load
                  :triggers #{:load}
                  :reaction (fn [this file-path]
                              (load-pdf this file-path)))


(object/object* :pdf-element
                :tags #{}
                :triggers #{}
                :behaviors [::load]
                :editor nil
                :init (fn [this]
                        [:canvas {:id (str "pdf-canvas-" (:id @this))
                                  :style {:border "1px solid gray"}}]))


(defn pdf-obj [] (object/create :pdf-element))