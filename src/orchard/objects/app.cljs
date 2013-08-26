(ns orchard.objects.app
  (:require-macros
    [orchard.macros :refer [defonce]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! timeout]]
    [orchard.object :as object]
    [orchard.model :as model]
    [orchard.util.time :refer [now]]
    [orchard.util.os :as os]
    [orchard.util.log :refer (log log-obj)]
    [orchard.util.dom :as dom]
    [orchard.util.core :as util]
    [orchard.objects.nav :as nav]
    [orchard.objects.logger :as logger]
    [orchard.objects.sidebar :as sidebar]
    [orchard.util.nw  :as nw]
    [orchard.dispatch :as dispatch]
    [orchard.objects.workspace :as workspace]
    orchard.kv-store
    orchard.objects.wiki-document))


(def gui (js/require "nw.gui"))
(def win (.Window.get gui))


(def closing true)

(declare app)

(def windows js/global.windows)

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
             {:label "New Project"}
             ]
     "Edit" [{:label "Find"}]
     "View" [{:label "Toggle Text Styles"}
             {:label "Status Bar"}
             ]]))


(defn open-window []
  (let [id (swap! js/global.windowsId inc)
        w (.Window.open gui "index.html" (clj->js {:toolbar false
                                                   :show false}))]
    (set! (.-window_id w) id)
    (swap! windows assoc id w)))


(defn save-session []
  (orchard.kv-store/local-set :session {:x (.-x win) :y (.-y win)
                                      :width (.-width win) :height (.-height win)}))

(defn restore-session []
  (if-let [sesh (js->clj (orchard.kv-store/local-get :session))]
    (let [sesh (into {} (for [[k v] sesh] [k (js/parseInt v)]))]
      (.moveTo win (:x sesh) (:y sesh))
      (.resizeTo win (:width sesh) (:height sesh)))))

(defn ready? [this]
  (= 0 (:delays @this)))

(defn open-document
  [doc-id]
  (go
    (let [doc (<! (model/load-document doc-id))]
      (object/raise workspace/workspace :show-document doc)
      (dispatch/fire :open-document @doc))))


(defn open-from-link
  [href]
  (log "open-from-link " href)
  (go
    (let [[project-title title] (clojure.string/split href #"/")
          all-docs     (<! (model/all-wiki-documents))
          projects     (reduce
                        (fn [m wiki-doc]
                          (let [proj (or (:project wiki-doc) "No Project")]
                            (assoc-in m [(clojure.string/lower-case proj) (clojure.string/lower-case (:title wiki-doc))]
                              wiki-doc)))
                        {}
                        all-docs)]
      (if-let [d (get-in projects [(clojure.string/lower-case project-title) (clojure.string/lower-case title)])]
        (open-document (:_id d))
        (log "Tried to open document that doesn't exist")))))


(object/behavior* ::refresh
  :triggers #{:refresh}
  :reaction (fn [this]
              (refresh)))


(object/behavior* ::start
  :triggers #{:start}
  :reaction (fn [this]
              ;(setup-tray)
              (nw/set-menu-bar! (main-menu))
              ; (restore-session)
              (.on win "close"
                   (fn []
                     (save-session)
                     (object/raise this :quit)
                     (this-as this (.close this true))))
              (log "Showing application window...")
              (object/raise orchard.objects.nav/workspace-nav :add!)
              ; (sidebar/init)
              ;; create resize handler
              (aset js/window "onresize" #(dispatch/fire :resize-window %))
              (.tooltip (js/$ ".sidebar-tab-item"))
              (open-document :home)
              (nw/show)))


(object/behavior* ::quit
  :triggers #{:quit}
  :reaction (fn [this]
              (log "Quitting...")
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

(defn init []
  (log "orchard.objects.app.init")
  (log "starting repl server...")
  (util/start-repl-server)
  (go
    (<! (model/load-db))
    (model/load-cache)
    (log "db ready, starting app")
    (logger/ready)
    (object/raise app :start)))

;(set! (.-workerSrc js/PDFJS) "js/pdf.js"))


;; Global Key events
(def last-key (atom nil))

(def ctrl-events
  { ;; logger show/hide
    12  orchard.objects.logger/toggle
    ;; navbar show/hide
    6   orchard.objects.nav/toggle
    ;; go home
    8   (partial orchard.objects.app/open-document :home)
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

