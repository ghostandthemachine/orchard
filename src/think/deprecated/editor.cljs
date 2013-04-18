(ns think.editor
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [clojure.browser.repl :as repl]
            [dommy.template :as tpl]
            [clojure.string :as string]
            [dommy.core :as dom]
            [dommy.attrs :as attrs]
            [think.dispatch :as dispatch]
            [think.view-helpers :as view]
            [think.util.log :refer [log]]
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
                          :split-pos 6
                          :last-single-editor-edit 0
                          :last-live-preview-editor-edit 0}))

(def single-editor* (atom nil))
(def live-preview-editor* (atom nil))

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
  [size id & content]
  [:div {:class (str "span" size) :id id}
    content])

(defn live-preview-component
  []
  [:div.row-fluid
    [:div.span6 {:id "live-preview-editor-pane"}
      [:form
        [:textarea {:id "live-preview-text-input"}]]]
    (preview-component 6 "live-preview-preview-pane")])

(defn tab-container-view
  []
  [:div.tab-content
    [:div.tab-pane {:id "present-tab"}
      (preview-component 12 "preview-pane")]
    [:div.tab-pane.active {:id "editor-input-tab"}
      (editor-component)]
    [:div.tab-pane {:id "live-preview-tab"}
      (live-preview-component)]])

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

(defn get-single-editor
  []
  (sel1 :#text-input))

(defn get-live-preview-editor
  []
  (sel1 :#live-preview-text-input))

(defn get-preview
  []
  (sel1 :#text-output))

(def update-delay 300)

(defn handle-update
  [editor preview]
  (when editor
    (let [editor-value (.getValue editor)
          value (if (> (count editor-value) 0)
                  editor-value
                  " ")
          rendered-elements (tpl/html->nodes
                          (js/markdown.toHTML value))
          preview-panel (tpl/node
                          (preview-component (- 12 (current-split-pos)) "live-preview-preview-pane"))]
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
  (let [preview-pane (dommy.template/node (preview-component init-span-size "preview-pane"))
        editor-pane  (sel1 :#editor-pane)
        editor-row   (sel1 :#editor-row)
        editor       @single-editor*]
    (aset editor-pane "className" (str "span" (- 12 init-span-size)))
    (dom/append! editor-row preview-pane)
    ; (js/setTimeout handle-update update-delay)
    ))

(defn init-view
  []
  (dom/replace! (sel1 :body)
    [:body.unselectable (editor-view)]))

(defn update-editor-text
  "Takes a map which should contain a :content associated text value
  and displays it in the current editor."
  ([page]
    (update-editor-text @single-editor* page))
  ([editor page]
  (.setValue editor (:content page))))


(defn get-current-editor-text
  []
  (.getValue @single-editor*))

(defn focused?
  [kw]
  (= (kw @editor-state*)))

(defn current-time
  []
  (.getTime (new js/Date)))

(defn set-last-edit-time
  [kw]
  (case kw
    :single-editor (swap! editor-state* merge {:last-single-editor-edit (current-time)})
    :live-preview-editor (swap! editor-state* merge {:last-live-preview-editor-edit (current-time)})))

(defn init-code-mirror
  []
  (let [single-editor-text-area (get-single-editor)
        live-preview-text-area  (get-live-preview-editor)
        single-editor           (CodeMirror/fromTextArea single-editor-text-area default-opts)
        live-preview-editor     (CodeMirror/fromTextArea live-preview-text-area default-opts)]
      (reset! single-editor* single-editor)
      (reset! live-preview-editor* live-preview-editor)

    (.on live-preview-editor "focus"
      (fn [e]
        (log (:last-live-preview-editor-edit @editor-state*) (:last-single-editor-edit @editor-state*))
        (swap! editor-state* assoc {:current :live-preview-editor})
        (when (> (:last-single-editor-edit @editor-state*) (:last-live-preview-editor-edit @editor-state*))
          (.setValue live-preview-editor (.getValue single-editor))
          (.refresh live-preview-editor))))

    (.on single-editor "focus"
      (fn [e]
        (log (:last-live-preview-editor-edit @editor-state*) (:last-single-editor-edit @editor-state*))
        (swap! editor-state* assoc {:current :single-editor})
        (when (> (:last-live-preview-editor-edit @editor-state*) (:last-single-editor-edit @editor-state*))
          (.setValue single-editor (.getValue live-preview-editor))
          (.refresh single-editor))))

    (.on live-preview-editor "change"
      (fn [e]
        (set-last-edit-time :live-preview-editor)
        (handle-update live-preview-editor (sel1 :#live-preview-preview-pane))))

    (.on live-preview-editor "change"
      (fn [e]
        (set-last-edit-time :single-editor)))

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
  ; (dom/listen! (sel1 "a[href=\"#live-preview-tab\"]")  :click handle-change-tab)
  (dom/listen! (sel1 "a[href=\"#present-tab\"]")  :click (partial handle-update @live-preview-editor* (sel1 :#preview-pane)))
  ; (dom/listen! (sel1 "a[href=\"#editor-input-tab\"]")  :click handle-change-tab)

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
    (let [editor (get-single-editor)]
      (.setValue @single-editor* (or data "empty content")))))