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

(defn get-editor
  []
  (sel1 :#text-input))

(defn get-preview
  []
  (sel1 :#text-output))

(def update-delay 300)

(defn handle-update
  [editor instance change]
  (when editor
    (let [preview (get-preview)
          editor-value (.getValue editor)
          value (if (> (count editor-value) 0)
                  editor-value
                  " ")
          rendered-elements (dommy-template/html->nodes
                          (js/markdown.toHTML value))
          preview-panel (dommy-template/node
                              [:div {:id "text-output"}])]
      (reduce #(dommy/append! %1 (dommy-template/node %2)) preview-panel rendered-elements)
      (dommy/replace! preview
        preview-panel))))

(defn handle-timeout
  []
  (js/clearTimeout update-delay)
  (js/setTimeout handle-update update-delay))

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
  [:div.navbar
    [:div.navbar-inner
      [:ul.nav
        [:li
          (dropdown-element "File" ["New" "Open" "Save" "Close"])]
        [:li
          (dropdown-element "Edit" ["New" "Open" "Save" "Close"])]]]])

(defn editor-view
  []
  [:div.container
    [:div.span12 {:id "edtior-container"}
      [:br]
      [:button.btn {:id "editor-save-btn" :style {:margin "10px"}} "Save"]
      [:br]
      [:form
        [:textarea {:id "text-input"}]]]])

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
      #(dispatch/fire :save-editor-text (get-current-editor-text))
      )
    ; (.on editor "change" (partial handle-update editor))
    ; (js/setTimeout handle-update update-delay)
    ))

(defn init
  []
  (start-repl-server)
  (init-view)
  (init-code-mirror))



(dispatch/react-to #{:save-editor-text} (fn [ev & [data]] (log data)))