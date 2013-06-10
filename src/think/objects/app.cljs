(ns think.objects.app
  (:use-macros [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.objects.context :as ctx]
            [think.model :as model]
            [think.dispatch :refer [react-to]]
            [think.util.js :refer [now]]
            [think.util.log :refer [log log-obj]]
            [think.util.dom  :as dom]
            [think.util :as util]
            [think.objects.nav :as nav]
            [think.util.nw  :as nw]
            [think.objects.workspace :as workspace]
            think.kv-store
            think.objects.wiki-document
            [redlobster.promise :as p]))

(def gui (js/require "nw.gui"))
(def win (.Window.get gui))


(def child-process (js/require "child_process"))

(def shell (.-exec child-process))

(def spawn (.-spawn child-process))


(defn log-handler
  [log-id msg]
  (object/raise think.objects.logger/logger :post log-id msg))


(def child-processes* (atom {}))

(defn add-process
  [child]
  (swap! child-processes* assoc (.-pid child) child))


(defn stop-children
  []
  (doseq [[pid child] @child-processes*]
    (log "kill pid" pid)
    (.kill child)))


(def closing true)

(declare app)

(def windows js/global.windows)


(defn start-couch-db
  []
  (let [proc (spawn "couchdb")]
    (add-process proc)
    (.on (.-stdout proc) "data" (partial log-handler :couchdb))
    proc))


(def couch (start-couch-db))


(defn start-cljsbuild
  []
  (let [proc (spawn "lein" (clj->js ["cljsbuild" "auto"]))]
    (add-process proc)
    (.on (.-stdout proc) "data" (partial log-handler :cljsbuild))
    proc))

(def cljsbuild (start-cljsbuild))


(defn setup-tray
  []
  "Creates a tray menu in upper-right app tray."
    (log "creating tray menu...")
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
  (let-realised [doc (model/load-document doc-id)]
    (object/raise workspace/workspace :show-document @doc)))



(object/behavior* ::ready
  :triggers #{:ready}
  :reaction (fn [this]
              (log "App ready")
              (object/raise think.objects.nav/workspace-nav :add!)
              (util/start-repl-server)
              (open-document :home)
              (object/raise think.objects.logger/logger :ready)))


(object/behavior* ::refresh
  :triggers #{:refresh}
  :reaction (fn [this]
              (log "Refresh App")
              (stop-children)
              (refresh)))


(object/behavior* ::init-window
  :triggers #{:init-window}
  :reaction (fn [this]
              (log "Start App")
              (setup-tray)
              (restore-session)
              (.on win "close"
                   (fn []
                     (save-session)
                     (object/raise this :quit)
                     (this-as this (.close this true))))
              (nw/show)
              (.showDevTools win)))

(object/behavior* ::quit
  :triggers #{:quit}
  :reaction (fn [this]
              (log "Quitting...")
              (stop-children)
              (nw/quit)))


(object/behavior* ::show-dev-tools
                  :triggers #{:show-dev-tools}
                  :reaction (fn [this]
                              (log "Show Dev Tools")
                              (.showDevTools win)))


(object/object* ::app
                :tags #{:app}
                :triggers [:quit :ready :show-dev-tools :init-window :refresh]
                :behaviors [::quit ::ready ::show-dev-tools ::init-window ::refresh]
                :delays 0
                :init (fn [this]
                        (ctx/in! :app this)))

; (object/tag-behaviors :app [::store-position-on-close ::restore-position-on-init ::restore-fullscreen])

(when-not js/global.windows
  (set! js/global.windows (atom (sorted-map 0 win)))
  (set! js/global.windowsId (atom 0))
  (set! (.-ltid win) 0))

(def windows js/global.windows)

(def app         (object/create ::app))


(defn init []
  (think.util/start-repl-server)
  (object/raise app :init-home)
  (set! (.-workerSrc js/PDFJS) "js/pdf.js"))




(object/raise app :init-window)