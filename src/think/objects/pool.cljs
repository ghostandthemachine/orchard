(ns think.objects.module.pool
  (:require [think.object :as object]
            [think.objects.app :as app]
            [think.objects.canvas :as canvas]
            ; [think.objects.editor :as editor]
            [think.util.cljs :refer [->dottedkw]]
            [think.util.dom :as dom])
  (:require-macros [think.macros :refer [defui]]))

; (defn get-all []
;   (object/instances-by-type module/mod-obj))

; (object/behavior* ::options-changed
;                   :triggers #{:options-changed}
;                   :reaction (fn [this opts]
;                               (doseq [ed (get-all)
;                                       :let [e (:ed @ed)]]
;                                 (editor/set-options e opts))))

; (object/object* ::pool
;                 :tags #{:module.pool})

; (object/tag-behaviors :module.pool [::options-changed])

; (defn unsaved? []
;   (some #(:dirty (deref %)) (object/instances-by-type module/mod-obj)))

; (defn by-path [path]
;   (filter #(= (-> @% :info :path) path) (object/by-tag :module)))

; (defui button [label & [cb]]
;        [:div.button.right label]
;        :click (fn []
;                 (when cb
;                   (cb))))

; (defn unsaved-prompt [on-yes]
;   (popup/popup! {:header "You will lose changes."
;                  :body "If you close now, you'll lose any unsaved changes. Are you sure you want to do that?"
;                  :buttons [{:label "Discard changes"
;                             :action on-yes}
;                            popup/cancel-button]}))

; (def pool (object/create ::pool))

; (object/behavior* ::track-active
;                   :triggers #{:active}
;                   :reaction (fn [this]
;                               (object/merge! pool {:last this})))

; (object/behavior* ::stop-close-dirty
;                   :triggers #{:close}
;                   :reaction (fn [this]
;                               (when (unsaved?)
;                                 (app/prevent-close)
;                                 (unsaved-prompt (partial app/close true)))))

; (object/behavior* ::stop-reload-dirty
;                   :triggers #{:reload}
;                   :reaction (fn [this]
;                               (when (unsaved?)
;                                 (app/prevent-close)
;                                 (unsaved-prompt app/refresh))))

; (object/behavior* ::ed-close
;                   :triggers #{:close}
;                   :reaction (fn [this]
;                               (if (:dirty @this)
;                                 (unsaved-prompt #(object/raise this :close.force))
;                                 (object/raise this :close.force))))

; (object/behavior* ::focus-last-on-focus
;                   :triggers #{:focus!}
;                   :reaction (fn [this]
;                               (focus-last)))

; (defn last-active []
;   (:last @pool))

; (defn focus-last []
;   (when-let [ed (last-active)]
;     (when-let [ed (:ed @ed)]
;       (dom/focus js/document.body)
;       (editor/focus ed))))

; (defn set-syntax [ed mode]
;   (let [prev-type (-> @ed :info :type)]
;     (object/update! ed [:info] assoc :type mode)
;     (editor/set-mode ed mode)
;     (when prev-type
;       (object/remove-tags ed [(->dottedkw :module prev-type)]))
;     (object/add-tags ed [(->dottedkw :module mode)])))

; (defn create [info & [wraps]]
;   (let [wraps (apply comp wraps)
;         ed (object/create (-> module/mod-obj wraps) info)]
;     (object/add-tags ed [(->dottedkw :module (:type info))])
;     (object/raise pool :create ed info)
;     ed))

; (object/add-behavior! canvas/canvas ::focus-last-on-focus)
; (object/tag-behaviors :app [::stop-close-dirty ::stop-reload-dirty])
; (object/tag-behaviors :module [::ed-close ::track-active])
