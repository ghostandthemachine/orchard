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
  (-elem [this] (seq this)))


(defn main-toolbar
  []
  [:div.row-fluid.button-bar          {:id "main-toolbar-row"}
   [:div.btn-group.editor-btn-nav     {:data-toggle "buttons-radio" :id "editor-tab"}
    [:a.btn.btn-small.view-btn-group  {:data-toggle "tab"
                                       :href "#present-tab"} 
                                      "Home"]]
   [:a.btn.btn-small.pull-right {:id "search-btn"} "Search"]])

(defview module-btn
  [this]
  [:div.module-btn]
  :click (fn [e] (fire :toggle-module this)))

(defview module
  [this & handlers]
  [:div.module
    (module-btn this)
    [:div.module-content]
      this]
  handlers)

(defrecord PDFDocument
  [type id rev created-at updated-at
   title authors path filename notes annotations cites tags])


(defrecord WikiDocument
  [type id rev created-at updated-at title template]
  dommy.template/PElement
  (-elem [this]
    [:div.document
      (tpl/-elem (:template this))]))

(extend-type model/MarkdownModule
  dommy.template/PElement
  (-elem [this]
    (module
      (reduce
        conj
        [:div.markdown-module]
        (tpl/html->nodes
          (js/markdown.toHTML (:text this)))))))


(extend-type model/HTMLModule
  dommy.template/PElement
  (-elem [this]
    (module
      (reduce conj
        [:div.html-module]
        (tpl/html->nodes (:text this))))))

(extend-type model/WikiDocument
  dommy.template/PElement
  (-elem [this]
    [:div.document
      (tpl/-elem (:template this))]))


(extend-type model/SingleColumnTemplate
  dommy.template/PElement
  (-elem [this]
    (vec (concat [:div.template.single-column-template]
                 (map tpl/-elem (:modules this))))))

(react-to
  #{:toggle-module}
  (fn [ev data] (js/alert "you clicked a module editor toggle")))


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

(defn go-home
  []
  (let-realised [doc (model/get-document :home)]
    (log "going home ...")
    (when-not (nil? @doc)
      (render-doc :#app-content @doc))))


(defn init
  []
  (start-repl-server)
  (model/init-document-db)
  (init-view)
  (react-to #{:document-db-ready} go-home))
