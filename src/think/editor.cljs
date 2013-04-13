(ns think.editor
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.template :as tpl]
            [clojure.string :as string]
            [dommy.core :as dom]
            [dommy.attrs :as attrs]
            [think.dispatch :as dispatch]
            [think.view-helpers :as view]
            [think.log :refer [log]]
            [think.project :as project]
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

(defn side-bar
  []
  )

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
  [:div.btn-group.editor-btn-nav {:data-toggle "buttons-radio" :id "editor-tab"}
    [:a.btn.btn-small.view-btn-group          {:data-toggle "tab"
                                               :href "#present-tab"} "Present"]
    [:a.btn.btn-small.view-btn-group.active   {:data-toggle "tab"
                                               :href "#editor-input-tab"} "Edit"]
    [:a.btn.btn-small.view-btn-group          {:data-toggle "tab"
                                               :href "#live-preview-tab"} "Live Preview"]])

(defn feature-nav
  []
  [:div.btn-group.pull-right.editor-btn-nav {:data-toggle "buttons-radio" :id "editor-tab"}
    [:a.btn.btn-small.split-pos-btn.active {:data-toggle "tab"
                                            :data-target "1:1"} "1:1"]
    [:a.btn.btn-small.split-pos-btn        {:data-toggle "tab"
                                            :data-target "3:1"} "3:1"]
    [:a.btn.btn-small.split-pos-btn        {:data-toggle "tab"
                                            :data-target "1:3"} "1:3"]])

(defn editor-component
  []
  [:form
    [:textarea {:id "text-input"}]])

(defn preview-component
  [size & content]
  [:div {:class (str "span" size) :id "preview-pane"}
    content])

(defn tab-container-view
  []
  [:div.tab-content
    [:div.tab-pane {:id "present-tab"}
      (preview-component 12)]
    [:div.tab-pane.active {:id "editor-input-tab"}
        (editor-component)]
    [:div.tab-pane {:id "live-preview-tab"}]])

(defn editor-view
  []

  [:div.main-container {:id "edtior-container"}
    [:div.row-fluid.button-bar
      (navbar)
      [:a.btn.btn-small.view-btn-group {:id "editor-save-btn"} "Save"]
      [:a.btn.btn-small.view-btn-group {:id "project-nav-btn"} "Projects"]
      (feature-nav)]
    [:div.row-fluid {:id "editor-row"}
      (tab-container-view)]])

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
    (dom/append! editor-row preview-pane)
    ; (.on editor "change" (partial handle-update editor preview-pane))
    (js/setTimeout handle-update update-delay)))

(defn init-view
  []
  (dom/replace! (sel1 :body)
    [:body.unselectable (editor-view)]))

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
    (.on editor "change" (partial handle-update editor (sel1 :#preview-pane)))
    (dom/listen!
      [(sel1 :body) :#editor-save-btn]
      :click
      #(dispatch/fire :save-editor-text (get-current-editor-text)))))

(def show-project-nav* (atom false))

(defn toggle-project-nav!
  []
  (reset! show-project-nav* (not @show-project-nav*)))


(defn open-nav
  [nav main-container]
  (log "open nav")
  (dom/set-style! nav :display "show")
  (dom/set-style! main-container :width "78%"))

(defn close-nav
  [nav main-container]
  (log "close nav")
  (dom/set-style! nav :display "none")
  (dom/set-style! main-container :width "98%"))


(defn project-nav-handler
  [nav main-container]
  (if (toggle-project-nav!)
    (open-nav nav main-container)
    (close-nav nav main-container)))

(defn get-nav
  []
  (sel1 :#project-nav))

(defn init-nav-btn-handlers
  []
  (dom/listen! (sel1 :#project-nav-btn)  :click (partial project-nav-handler (get-nav) (sel1 :.main-container)))
  (dom/listen! (sel1 :#present-toggle)   :click (partial handle-update (get-editor) (sel1 :#preview-pane)))
  ; (dom/listen! (sel1 :#live-preview-toggle) :click show-live-preview-handler)
  )

(defn init
  []
  (start-repl-server)
  (init-view)
  (init-code-mirror)
  (project/init)
  (init-nav-btn-handlers))



(dispatch/react-to #{:load-project}
  (fn [ev & [data]]
    (let [editor (get-editor)]
      (.setValue @editor* (or data "empty content")))))