(ns orchard.objects.editable.editor
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require [orchard.util.dom :as dom]
            [orchard.editable.toolbar :as toolbar]
            [orchard.observe :as observe]
            [orchard.objects.editable.core :refer [event-chan make-editable! selected-node]]
            [crate.core :as crate]
            [orchard.util.log :refer (log log-obj)]
            [orchard.util.core :refer (js-style-name)]
            [cljs.core.async :refer [chan >! <! timeout put! get!]]))


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
      (let [ev (<! (:mouse-down-chan channels))]
        (log-obj (selected-node))
        (toggle-bold-btn editor))
      (recur))))

(defn mouse-up-handler
  [{:keys [channels] :as editor}]
  (go
    (loop []
      (let [ev (<! (:mouse-up-chan channels))]
        (log-obj (selected-node))
        (toggle-bold-btn editor)
      (recur)))))


(defn change-chan [ed]
  (let [c (chan)]
    (observe/observe ed
      (fn [mutations]
        (for [m mutations]
          (put! c m)))
      :child-list
      :attributes
      :character-data
      :subtree)
    c))


(def default-opts
  {:editor-height "100px"})


(defn editor
  [sel & opts]
  (let [opts              (merge default-opts (apply hash-map (flatten (partition 2 opts))))
        ed                (dom/$ sel)
        ;; channels
        mouse-click-chan  (event-chan ed :click)
        mouse-down-chan   (event-chan ed :mousedown)
        mouse-up-chan     (event-chan ed :mouseup)
        blur-chan         (event-chan ed :blur)
        focus-chan        (event-chan ed :focus)
        change-chan       (change-chan ed)

        toolbar           (crate/html (toolbar/view))
        ;; return the entire state of this editor
        editor            (merge opts
                            {:editor      ed
                             :toolbar     toolbar
                             :channels   {:blur-chan          blur-chan
                                          :focus-chan         focus-chan
                                          :mouse-click-chan   mouse-click-chan
                                          :mouse-down-chan    mouse-down-chan
                                          :mouse-up-chan      mouse-up-chan}})]
    ;; add toolbar
    (dom/before (dom/$ sel) toolbar)
    ;; look and feel
    (dom/set-css ed {:height (:editor-height opts)})
    ;; functionality
    (make-editable! ed)

    (go
      (while true
        (let [change (<! change-chan)]
          (.log js/console change))))

    (go
      (let [ev (<! change-chan)]
        (log ev)))
    editor))