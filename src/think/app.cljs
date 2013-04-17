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
  [id record]
  [:div.module-btn]
  :click (fn [e] (fire :toggle-module [id record])))

(defview module
  [content record & handlers]
  (let [id (uuid)]
    [:div.module {:id (:id id)}
      (module-btn id record)
      [:div.module-content]
        content])
  handlers)

(defn editor-component
  [id record]
  (module
    [:form
      [:textarea {:id (str "editor-" id)}]]
    record))

(defrecord PDFDocument
  [type id rev created-at updated-at
   title authors path filename notes annotations cites tags])


(defrecord WikiDocument
  [type id rev created-at updated-at title template])


(extend-type WikiDocument
  dommy.template/PElement
  (-elem [this]
    [:div.document
      (tpl/-elem (:template this))]))


(extend-type model/MarkdownModule
  dommy.template/PElement
  (-elem [this]
    (log this)
    (module
      (reduce
        conj
        [:div.markdown-module]
        (tpl/html->nodes
          (js/markdown.toHTML (:text this))))
      this)))


(extend-type model/HTMLModule
  dommy.template/PElement
  (-elem [this]
    (module
      (reduce conj
        [:div.html-module]
        (tpl/html->nodes (:text this)))
      this)))

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
  (fn [ev [id record]]
    (log "Event type: " ev " id: " id " record: " record)))


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


(def bar (atom nil))

(defn init
  []
  (start-repl-server)
  (model/init-document-db)
  (init-view)
  (react-to #{:document-db-ready} go-home))
