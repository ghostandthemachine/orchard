(ns think.objects.app
  (:require-macros
    [think.macros :refer [defonce]]
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! timeout]]
    [think.object :as object]
    [think.model :as model]
    [think.util.time :refer [now]]
    [think.util.os :as os]
    [think.util.log :refer (log log-obj)]
    [think.util.dom  :as dom]
    [think.util.core :as util]
    [think.objects.nav :as nav]
    [think.objects.sidebar :as sidebar]
    [think.util.nw  :as nw]
    [think.dispatch :as dispatch]
    [think.objects.workspace :as workspace]
    think.kv-store
    think.objects.wiki-document))


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


(defn set-window-menu
  []
  (let [menu (.Menu gui
                (clj->js
                  {:type "menubar"}))
        win (.Window.get gui)]
    (set! (.-menu win) menu)))


(defn open-window []
  (let [id (swap! js/global.windowsId inc)
        w (.Window.open gui "index.html" (clj->js {:toolbar false
                                                   :show false}))]
    (set! (.-window_id w) id)
    (swap! windows assoc id w)))


(defn save-session []
  (think.kv-store/local-set :session {:x (.-x win) :y (.-y win)
                                      :width (.-width win) :height (.-height win)}))

(defn restore-session []
  (if-let [sesh (js->clj (think.kv-store/local-get :session))]
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


(object/behavior* ::refresh
  :triggers #{:refresh}
  :reaction (fn [this]
              (refresh)))


(object/behavior* ::start
  :triggers #{:start}
  :reaction (fn [this]
              (setup-tray)
              ; (restore-session)
              (.on win "close"
                   (fn []
                     (save-session)
                     (object/raise this :quit)
                     (this-as this (.close this true))))
              (log "Showing application window...")
              (object/raise think.objects.nav/workspace-nav :add!)
              (sidebar/init)
              ;; create resize handler
              (aset js/window "onresize" #(dispatch/fire :resize-window %))

              (.tooltip (js/$ ".sidebar-tab-item"))
              (open-document :home)
              (nw/show)))


(object/behavior* ::quit
  :triggers #{:quit}
  :reaction (fn [this]
              (log "Quitting...")
              (object/raise think.objects.logger/logger :quit)
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
  (log "think.objects.app.init")
  (log "starting repl server...")
  (util/start-repl-server)
  (.showDevTools win)
  (go
    (<! (model/load-db))
    (log "db ready, starting app")
    (object/raise app :start)))

;(set! (.-workerSrc js/PDFJS) "js/pdf.js"))
