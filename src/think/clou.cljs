(ns think.clou
  (:use-macros [dommy.macros :only [sel]])
  (:require [clojure.browser.repl :as repl]
            [dommy.template :as dommy-template]
            [clojure.string :as string]
            [dommy.core :as dommy]))

(defn clou-view
  []
    [:h1 "Clou"])


(defn log [v & text]
  (let [vs (if (string? v)
             (apply str v text)
             v)]
    (. js/console (log vs))))

(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"}))

(defn get-editor
  []
  (first (sel :#text-input)))

(defn get-preview
  []
  (first (sel :#text-output)))

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

(defn init-code-mirror
  []
  (let [text-area (get-editor)
        editor (CodeMirror/fromTextArea text-area default-opts)]
    (.on editor "change" (partial handle-update editor))
    (js/setTimeout handle-update update-delay)))

(defn start-repl-server [] (repl/connect "http://localhost:9000/repl"))

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

(defn view
  []
  [:div.container-fluid {:id "content"}
    [:div.row-fluid
      [:div.span6 {:id "left-text"}
        [:form
          [:textarea {:id "text-input"} "Type some markdown here"]]]
      [:div.span6 {:id "right-text"}
        [:div {:id "text-output"}]]]])
