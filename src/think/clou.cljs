(ns think.clou
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.template :as tpl]
            [clojure.string :as string]
            [dommy.core :as dom]
            [dommy.attrs :as attrs]
            [think.dispatch :as dispatch]
            [think.view-helpers :as view]
            [think.log :refer [log]]
            [think.util :refer [start-repl-server]]))

(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true}))

(def editor-state* (atom {:current :editor
                          :split-pos 6}))

(def editor* (atom nil))

(defn generate-id-from-name
  [n]
  (str (string/lower-case (string/replace n #" " "-")) "-menu-item"))

(defn dropdown-element
  [title child-elements]
  [:div.btn-group
    [:btn.btn.btn-small.dropdown-toggle {:data-toggle "dropdown" :href "#"}
      title]
    [:ul.dropdown-menu
      (map
        (fn [t]
          [:li
            [:a {:href (str "#" t) :class (generate-id-from-name t)} t]])
        child-elements)]])

(defn navbar
  []
  [:div.btn-group.editor-btn-nav
    [:button.btn.btn-small.view-btn-group.active {:id "editor-view-toggle"} "Editor"]
    [:button.btn.btn-small.view-btn-group        {:id "preview-toggle"} "Preview"]
    [:button.btn.btn-small.view-btn-group        {:id "live-preview-toggle"} "Live Preview"]
    [:button.btn.btn-small.view-btn-group        {:id "editor-save-btn"} "Save"]])

(defn feature-nav
  []
  [:div.btn-group.pull-right.editor-btn-nav
    [:button.btn.btn-small.split-pos-btn.active {:id "editor-view-toggle"} "1:1"]
    [:button.btn.btn-small.split-pos-btn        {:id "live-preview-toggle"} "3:1"]
    [:button.btn.btn-small.split-pos-btn        {:id "editor-save-btn"} "1:3"]])


(defn editor-component
  []
  [:form
    [:textarea {:id "text-input"}]])

(defn editor-view
  []
  [:div.container-fluid {:id "edtior-container"}
    [:div.row-fluid.button-bar
      (navbar)
      (feature-nav)]
    [:div.row-fluid {:id "editor-row"}
      [:div.span12 {:id "editor-pane"}
        (editor-component)]]])

(defn preview-component
  [size & content]
  [:div {:class (str "span" size) :id "preview-pane"}
    content])

(defn current-split-pos [] (:split-pos @editor-state*))

(defn get-editor
  []
  (sel1 :#text-input))

(defn get-preview
  []
  (sel1 :#text-output))

(def update-delay 300)

(defn handle-update
  [editor _ _]
  (when editor
    (let [preview (sel1 :#preview-pane)
          editor-value (.getValue editor)
          value (if (> (count editor-value) 0)
                  editor-value
                  " ")
          rendered-elements (tpl/html->nodes
                          (js/markdown.toHTML value))
          preview-panel (tpl/node
                          (preview-component (- 12 (current-split-pos))))]
      (log "should be rendering preview")
      (reduce #(dom/append! %1 (tpl/node %2)) preview-panel rendered-elements)
      (dom/replace! preview
        preview-panel))))

(defn handle-timeout
  []
  (js/clearTimeout update-delay)
  (js/setTimeout handle-update update-delay))

(defn remove-preview-pane
  []
  (let [preview-pane (sel1 :#preview-pane)
        editor-pane (sel1 :#editor-pane)]
    (dom/remove! preview-pane)
    (aset editor-pane "className" "span12")))

(defn add-preview-pane
  [init-span-size]
  (let [preview-pane (dommy.template/node (preview-component init-span-size))
        editor-pane  (sel1 :#editor-pane)
        editor-row   (sel1 :#editor-row)
        editor       @editor*]
    (aset editor-pane "className" (str "span" (- 12 init-span-size)))
    (log "should append preview")
    (dom/append! editor-row preview-pane)
    (.on editor "change" (partial handle-update editor preview-pane))
    (js/setTimeout handle-update update-delay)))

(defn init-view
  []
  (dom/replace! (sel1 :body)
    [:body (editor-view)]))

(defn update-editor-text
  "Takes a map which should contain a :content associated text value
  and displays it in the current editor."
  ([page]
    (update-editor-text @editor* page))
  ([editor page]
  (.setValue editor (:content page))))


(defn get-current-editor-text
  []
  (.getValue @editor*))

(defn init-code-mirror
  []
  (let [text-area (get-editor)
        editor (CodeMirror/fromTextArea text-area default-opts)]
      (reset! editor* editor)
    (dom/listen!
      [(sel1 :body) :#editor-save-btn]
      :click
      #(dispatch/fire :save-editor-text (get-current-editor-text)))))


(defn toggle-split-btns
  [bool]
  (for [btn (sel :.split-pos-btn)]
    (dom/set-attr! btn :disabled bool)))

(defn show-editor
  []
  (aset (sel1 :#editor-pane) "className" "span12")
  (when (sel1 :#preview-pane)
    (dom/remove! (sel1 :#preview-pane)))
  (toggle-split-btns false))

(defn show-preview
  []
  (dom/toggle! (sel1 :#editor-pane) false)
  (add-preview-pane (:split-pos @editor-state*))
  (toggle-split-btns false))

(defn show-live-preview
  []
  (aset (sel1 :#editor-pane) "className" "span12")
  (add-preview-pane (:split-pos @editor-state*))
  (toggle-split-btns true))

(defn update-editor
  [state]
  (case (:current state)
    :editor      show-editor
    :preview     show-preview
    :live-preview show-live-preview
    show-editor))

(defn set-active-btn
  [target-id]
  (for [btn (sel :.view-btn-group)]
    (dom/remove-class! btn "active"))
  (dom/add-class! (sel1 target-id) "active"))

(defn show-live-preview-handler
  [_]
  (log "Show live preview")
    (set-active-btn :#live-preview-toggle)
    (update-editor
      (swap! editor-state* merge
        {:current :live-preview})))


(defn show-preview-handler
  [_]
  (log "Show Preview")
    (set-active-btn :#preview-toggle)
    (update-editor
      (swap! editor-state* merge
        {:current :preview
         :split-pos 0})))


(defn show-editor-handler
  [_]
  (log "Show editor")
    (set-active-btn :#editor-view-toggle)
    (update-editor
      (swap! editor-state* merge
        {:current :editor
         :split-pos 12})))


(defn init-nav-btn-handlers
  []
  (dom/listen! (sel1 :#editor-view-toggle)  :click show-editor-handler)
  (dom/listen! (sel1 :#preview-toggle)      :click show-preview-handler)
  (dom/listen! (sel1 :#live-preview-toggle) :click show-live-preview-handler))

(defn init
  []
  (start-repl-server)
  (init-view)
  (init-code-mirror)
  (init-nav-btn-handlers))



; (dispatch/react-to #{:save-editor-text} (fn [ev & [data]] (log data)))