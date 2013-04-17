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
  [record]
  [:div.module-btn]
  :click (fn [e] (fire :toggle-module record)))

(defview module
  [content record & handlers]
  [:div.module {:id (:id record)}
    (module-btn record)
    [:div.module-content]
      content]
  handlers)


; CodeMirror functions

(def editors* (atom {}))

(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true}))


(defn create-code-mirror
  [editor-id]
  (CodeMirror/fromTextArea
    (sel1 (str "#editor-" editor-id))
    default-opts))


(defn create-cm-module
  [record]
  (let [module (module [:textarea {:id (str "editor-" (:id record))}] record)
        id     (:id record)]
    (dom/replace! (sel1 (str "#" id))
      module)
    (let [cm-instance (create-code-mirror id)]
      (.setValue cm-instance (:text record))
      (swap! editors* assoc id {:module module
                                :cm-instance cm-instance}))))


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
    (swap! editors* assoc (:id this) nil)
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
    (swap! editors* assoc (:id this) nil)
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


(def document* (atom {}))

(def modules* (atom {}))

(defn load-doc
  [doc]
  (reset! document* doc)
  (reset! modules*
    (into {}
      (map (fn [mod] [(uuid) mod]) (get-in doc [:template :modules]))))
  (render-doc :#app-content doc))


(defn go-home
  []
  (let-realised [doc (model/get-document :home)]
    (log "going home ...")
    (when-not (nil? @doc)
      (load-doc @doc))))


(defn init
  []
  (start-repl-server)
  (model/init-document-db)
  (init-view)
  (react-to #{:document-db-ready} go-home))


(defn save-and-swap
  [record]
  (let [id   (:id record)
        editor-module (get-in @editors* [id :module])
        cm-instance (get-in @editors* [id :cm-instance])
        text (.getValue cm-instance)]
    (swap! editors* dissoc id)
    (dom/replace! editor-module
      record)))

(react-to
  #{:toggle-module}
  (fn [ev record]
    (if (nil? ((:id record) @editors*))
      (create-cm-module record)
      (save-and-swap record))))