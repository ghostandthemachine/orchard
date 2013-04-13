(ns think.clou
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.template :as dommy-template]
            [clojure.string :as string]
            [dommy.core :as dommy]
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
    [:button.btn.btn-small.active {:id "editor-view-toggle"} "Editor"]
    [:button.btn.btn-small        {:id "preview-toggle"} "Preview"]
    [:button.btn.btn-small        {:id "live-preview-toggle"} "Live Preview"]
    [:button.btn.btn-small        {:id "editor-save-btn"} "Save"]])

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

(def current-split-pos* (atom 6))

(defn set-split-pos
  [pos]
  (reset! current-split-pos* pos))

(defn current-split-pos [] @current-split-pos)

(defn get-editor
  []
  (sel1 :#text-input))

(defn get-preview
  []
  (sel1 :#text-output))

(def update-delay 300)

(defn handle-update
  [editor preview _ _]
  (when editor
    (let [editor-value (.getValue editor)
          value (if (> (count editor-value) 0)
                  editor-value
                  " ")
          rendered-elements (dommy-template/html->nodes
                          (js/markdown.toHTML value))
          preview-panel (dommy-template/node
                          (preview-component (- 12 (current-split-pos))))]
      (reduce #(dommy/append! %1 (dommy-template/node %2)) preview-panel rendered-elements)
      (dommy/replace! preview
        preview-panel))))

(defn handle-timeout
  []
  (js/clearTimeout update-delay)
  (js/setTimeout handle-update update-delay))

(def editor* (atom nil))


(defn remove-preview-pane
  []
  (let [preview-pane (sel1 :#preview-pane)
        editor-pane (sel1 :#editor-pane)]
    (dommy/remove! preview-pane)
    (aset editor-pane "className" "span12")))

(defn add-preview-pane
  [init-span-size]
  (let [preview-pane (dommy.template/node (preview-component init-span-size))
        editor-pane  (sel1 :#editor-pane)
        editor-row   (sel1 :#editor-row)
        editor       @editor*]
    (aset editor-pane "className" (str "span" (- 12 init-span-size)))
    (dommy/append! editor-row preview-pane)
    (.on editor "change" (partial handle-update editor preview-pane))
    (js/setTimeout handle-update update-delay)))

(defn init-view
  []
  (dommy/replace! (sel1 :body)
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
    (dommy/listen!
      [(sel1 :body) :#editor-save-btn]
      :click
      #(dispatch/fire :save-editor-text (get-current-editor-text)))))

(def editor-state* (atom {}))

(defn toggle-split-btns
  [bool]
  (for [btn (sel :.split-pos-btn)]
    (dommy/set-attr! btn :disabled bool)))

(defn show-editor
  []
  (aset (sel1 :#editor-pane) "className" "span12")
  (when (sel1 :#preview-pane)
    (dommy/remove! (sel1 :#preview-pane)))
  (toggle-split-btns false))

(defn show-live-preview
  []
  (aset (sel1 :#editor-pane) "className" "span12")
  (when (sel1 :#preview-pane)
    (dommy/remove! (sel1 :#preview-pane)))
  (toggle-split-btns true))

(defn update-editor
  [state]
  (case (:current state)
    :editor show-editor
    :live-prview show-live-preview))

(defn show-editor-handler
  [_]
  (log "Show editor")
  (when-not (= (:current @editor-state*) :editor)
    (update-editor
      (swap! editor-state* merge
        {:current :editor
         :split-pos 12}))))


(defn init-nav-btn-handlers
  []
  (dommy/listen! (sel1 :#editor-view-toggle) :click show-editor-handler))

(defn init
  []
  (start-repl-server)
  (init-view)
  (init-code-mirror)
  (init-nav-btn-handlers))



(dispatch/react-to #{:save-editor-text} (fn [ev & [data]] (log data)))
