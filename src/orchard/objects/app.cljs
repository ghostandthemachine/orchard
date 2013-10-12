(ns orchard.objects.app
  (:refer-clojure :exclude [defonce])
  (:require-macros
    [orchard.macros :refer [defonce]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async           :refer [chan >! <! timeout]]
    [orchard.object            :as object]
    [orchard.model             :as model]
    [orchard.kv-store          :as kv]
    [orchard.setup             :as setup]
    [orchard.util.time         :refer [now]]
    [orchard.util.os           :as os]
    [orchard.util.log          :refer (log log-obj)]
    [orchard.util.dom          :as dom]
    [orchard.util.core         :as util]
    [orchard.util.time         :as time]
    [orchard.objects.nav       :as nav]
    [orchard.objects.logger    :as logger]
    [orchard.objects.sidebar   :as sidebar]
    [orchard.util.nw           :as nw]
    [orchard.dispatch          :as dispatch]
    [orchard.objects.workspace :as workspace]
    orchard.objects.project
    orchard.objects.wiki-page))


(defonce db  nil)

(defonce gui (js/node_require "nw.gui"))
(defonce win (.Window.get gui))

(defonce closing true)

(declare app)

(defonce windows js/global.windows)

(defn setup-tray
  []
  "Creates a tray menu in upper-right app tray."
    (nw/tray! {:title "Thinker"
               :menu (nw/menu [{:label "Show"         :onclick #(.show (nw/window))}
                               {:type "separator"}
                               {:label "Quit"         :onclick #(object/raise app :quit)}])}))


(defn prevent-close []
  (set! closing false))


(defn close [force?]
  (when force?
    (object/raise app :closed))
  (.close win force?))


(defn refresh []
  (js/window.location.reload true))


(defn main-menu
  []
  (nw/menu-bar
    [;"Thinker" [{:label "Preferences"}]
     "File" [{:label "New Page"
              :click (fn [& args] (log "New Page clicked!") (log-obj args))}
             {:label "New Project"}]
     "Edit" [{:label "Find"}]
     "View" [{:label "Toggle Text Styles"}
             {:label "Status Bar"}]]))


(defn open-window []
  (let [id (swap! js/global.windowsId inc)
        w (.Window.open gui "index.html" (clj->js {:toolbar false
                                                   :show false}))]
    (set! (.-window_id w) id)
    (swap! windows assoc id w)))


(defn save-session []
  (kv/local-set :session {:x (.-x win) :y (.-y win)
                                      :width (.-width win) :height (.-height win)}))


(defn restore-session []
  (if-let [sesh (js->clj (kv/local-get :session))]
    (let [sesh (into {} (for [[k v] sesh] [k (js/parseInt v)]))]
      (.moveTo win (:x sesh) (:y sesh))
      (.resizeTo win (:width sesh) (:height sesh)))))


(defn ready? [this]
  (= 0 (:delays @this)))


(defn open-page
  [db page-id]
  (go
    (let [doc (<! (model/load-object db page-id))]
      (object/raise workspace/workspace :show-page doc))))


(defn show-project
  [db id]
  (go
    (let [project (<! (model/get-object db id))]
      (open-page db (:root project)))))


(defn open-from-link
  [href]
  (log "open-from-link " href)
  (go
    (let [[project-title title] (clojure.string/split href #"/")
          all-docs     (<! (model/all-wiki-pages db))
          projects     (reduce
                        (fn [m wiki-page]
                          (let [proj (or (:project wiki-page) "No Project")]
                            (assoc-in m [(clojure.string/lower-case proj) (clojure.string/lower-case (:title wiki-page))]
                              wiki-page)))
                        {}
                        all-docs)]
      (if-let [d (get-in projects [(clojure.string/lower-case project-title) (clojure.string/lower-case title)])]
        (open-page db (:_id d))
        (log "Tried to open document that doesn't exist")))))


(object/behavior* ::refresh
  :triggers #{:refresh}
  :reaction (fn [this]
              (refresh)))


(object/behavior* ::start
  :triggers #{:start}
  :reaction (fn [this db]
              ;(setup-tray)
              (nw/set-menu-bar! (main-menu))
              ; (restore-session)
              (.on win "close"
                   (fn []
                     (save-session)
                     (object/raise this :quit)
                     (this-as this (.close this true))))
              (object/raise orchard.objects.nav/workspace-nav :add!)
              ; (sidebar/init)

              ;; create resize handler
              (aset js/window "onresize" #(dispatch/fire :resize-window %))
              (.tooltip (js/$ ".sidebar-tab-item"))
              (nw/show)
              (show-project db :home)))


(object/behavior* ::quit
  :triggers #{:quit}
  :reaction (fn [this]
              (os/kill-children)
              (nw/quit)))


(object/behavior* ::show-dev-tools
                  :triggers #{:show-dev-tools}
                  :reaction (fn [this]
                              (.showDevTools win)))


(object/object* ::app
                :tags #{:app}
                :triggers [:quit :show-dev-tools :start :refresh]
                :behaviors [::quit ::show-dev-tools ::start ::refresh]
                :delays 0
                :init (fn [this]))


(when-not js/global.windows
  (set! js/global.windows (atom (sorted-map 0 win)))
  (set! js/global.windowsId (atom 0))
  (set! (.-ltid win) 0))

(def windows js/global.windows)

(def app (object/create ::app))

;(set! (.-workerSrc js/PDFJS) "js/pdf.js"))

;; Global Key events
(def last-key (atom nil))

(def ctrl-events
  { ;; logger show/hide
    12  orchard.objects.logger/toggle
    ;; navbar show/hide
    6   orchard.objects.nav/toggle
    ;; go home
    8   (partial orchard.objects.app/open-page :home)
    ;; refesh
    18  (partial object/raise orchard.objects.app/app :refresh)
    ;; create new doc
    14  (partial nav/start-create-document nav/workspace-nav)
    ;; show dev-tools
    4   (partial object/raise orchard.objects.app/app :show-dev-tools)})


(def ctrl-shift-events
  { ;; logger show/hide
    14 (partial nav/start-create-project nav/workspace-nav)})


(defn handle-keypress
  [e]
  (let [key-code (.-keyCode e)]
    ; (log-obj e)
    (when (.-ctrlKey e)
        (if (.-shiftKey e)
          (when (util/has? (keys ctrl-shift-events) key-code)
            (let [f (get ctrl-shift-events key-code)]
              (f)))

          (when (util/has? (keys ctrl-events) key-code)
            (let [f (get ctrl-events key-code)]
              (f)))))))


(aset js/window "onkeypress" handle-keypress)

(defn body
  []
  (.querySelector js/window.document "body"))

(defn load-js-file
  [path]
  (let [js-file (.createElement js/document "script")]
    (aset js-file "src" path)
    (aset js-file "type" "text/javascript")
    (aset js-file "async" false)
    (.appendChild (body) js-file)
    js-file))

(defn init-aloha
  []
  (aset js/window "requireNode" js/window.require)
  (aset js/window "require" nil)
  (load-js-file "js/aloha/require.js")
  (load-js-file "js/aloha/aloha.js"))


(def APP-INFO {:id      :app-info
               :version 0.1})

(defn init []
  (log "dev tools")
  (.showDevTools win)
  (nw/show)
  (log "repl server")
  (util/start-repl-server)
  (set! db (kv/local-store))
  (go
   (let [app-info (kv/local-get :app-info)]
     (log "app-info: " app-info)
     (when (or (nil? app-info)
               (not= (:version app-info) (:version APP-INFO)))
       (log "Clearing DB...")
       (log "start checking home")
       (<! (setup/check-home db))
       (log "done checking home")
       (kv/local-set :app-info APP-INFO)))
    (log "Prepare logger")
    (logger/ready)
    (log "Init Aloha")
    (init-aloha)
    (log "Ready to start app...")
    (object/raise app :start db)))
