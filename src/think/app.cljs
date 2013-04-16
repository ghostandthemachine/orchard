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


(extend-protocol tpl/PElement
  js/Array
  (-elem [this] (seq this))

  ; model/MarkdownModule
  ; (-elem [this]
  ;   (tpl/-elem
  ;     [:div.module.markdown-module
  ;      (second (js/markdown.toHTMLTree (:text this)))]))

  ; model/SingleColumnTemplate
  ; (-elem [this]
  ;   (tpl/-elem
  ;     [:div.template.single-column-template
  ;      (map tpl/-elem (:modules this))]))

  ; model/WikiDocument
  ; (-elem [this]
  ;   (tpl/-elem
  ;     [:div.document
  ;      (tpl/-elem (:template this))]))
)


(defn main-toolbar
  []
  [:div.row-fluid.button-bar {:id "main-toolbar-row"}
    [:div.btn-group.editor-btn-nav {:data-toggle "buttons-radio" :id "editor-tab"}
      [:a.btn.btn-small.view-btn-group          {:data-toggle "tab"
                                                 :href "#present-tab"} "Home"]]
    [:a.btn.btn-small.pull-right {:id "search-btn"} "Search"]])

(defrecord PDFDocument
  [type id rev created-at updated-at
   title authors path filename notes annotations cites tags])

(defrecord WikiDocument
  [type id rev created-at updated-at title template]
  dommy.template/PElement
  (-elem [this]
    [:div.document
      (tpl/-elem (:template this))]))

(react-to #{:toggle-module} (fn [ev data] (js/alert "you clicked a module editor toggle")))


(defn app-view
  []
  [:div.main-container {:id "app-container"}
    (main-toolbar)
   [:div.row-fluid {:id "app-content"}]])


(defn render-doc
  [tgt doc]
  (dom/append! (sel1 tgt)
    (tpl/node doc)))


(defn init-view
  []
  (dom/replace! (sel1 :body)
    [:body (app-view)]))


(defn init
  []
  (start-repl-server)
  (model/init-document-db)
  (init-view)
  (react-to #{:document-db-ready}
    (fn [_ _]
      (let-realised [doc (model/get-document :home)]
        (log "rendering home ...")
        (when-not (nil? @doc)
          (render-doc :#app-content @doc))))))
