(ns think.app
  (:use-macros [redlobster.macros :only [when-realised let-realised]]
               [dommy.macros :only [sel sel1]]
               [think.macros :only [defview]])
  (:require [redlobster.promise :refer [promise on-realised]]
            [think.dispatch :refer [fire react-to]]
            [think.model :as model]
            [think.log :refer [log log-obj log-err]]
            [think.util :refer [start-repl-server uuid ready refresh r! clipboard read-clipboard open-window editor-window]]
            [think.view-helpers :as view]
            [dommy.template :as tpl]
            [dommy.core :as dom]
            [redlobster.promise :as p]))


(defprotocol IRenderable
  (render [this] "Returns a hiccup form."))

(extend-type model/MarkdownModule
  IRenderable
  (render [this]
    [:div.module.markdown-module
      (tpl/html->nodes (js/markdown.toHTML (:text this)))]))


(extend-type model/SingleColumnTemplate
  IRenderable
  (render [this]
    [:div.template.single-column-template
      (map render (:modules this))]))


(extend-type model/WikiDocument
  IRenderable
  (render [this]
    [:div.document
      (render (:template this))]))


(defn main-toolbar
  []
  [:div.row-fluid.button-bar {:id "main-toolbar-row"}
    [:div.btn-group.editor-btn-nav {:data-toggle "buttons-radio" :id "editor-tab"}
      [:a.btn.btn-small.view-btn-group          {:data-toggle "tab"
                                                 :href "#present-tab"} "Home"]]
    [:a.btn.btn-small.pull-right {:id "search-btn"} "Search"]])

(defview module-btn
  [this]
  [:div.module-btn]
  :click (fn [e] (fire :toggle-module this)))

(defview module
  [this content & handlers]
  [:div.module
    (module-btn this)
    [:div.module-content]
      content]
  handlers)

(defn test-module
  []
  (module
    nil
    [:div.container
      [:h2 "Look at me, a module!"]]))

(react-to #{:toggle-module} (fn [ev data] (js/alert "you clicked a module editor toggle")))

(defn home-view
  [& content]
  [:div.row-fluid {:id "home-row"} content])


(defn app-view
  []
  [:div.main-container {:id "app-container"}
    (main-toolbar)
    (home-view (test-module))])


(defn render-doc
  [tgt doc]
  (let [target (sel1 tgt)]
    (dom/replace! target
      (home-view
        (render doc)))))


(defn init-content
  []
  (let-realised [doc (model/get-document :home)]
    (render-doc :#home-row @doc)))


(defn init-view
  []
  (dom/replace!  (sel1 :body)
    [:body (app-view)]))


(defn init
  []
  (start-repl-server)
  (model/init-document-db)
  (init-view)
  (react-to #{:document-db-ready}
    (fn [_ _]
      (init-content)))
  )
