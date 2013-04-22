(ns think.objects.app
  (:use-macros [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.objects.context :as ctx]
            [think.model :as model]
            [think.dispatch :refer [react-to]]
            [think.util.js :refer [now]]
            [think.util.log :refer [log]]
            [think.util.dom :refer [$ html append] :as dom]
            [think.util.nw  :as nw]
            [think.objects.workspace :as workspace]
            [redlobster.promise :as p]))

(def gui (js/require "nw.gui"))
(def win (.Window.get gui))
(def closing true)

(declare app)
(declare windows)

;(.Window.open gui "index.html" (clj->js {:toolbar false}))

(defn prevent-close []
  (set! closing false))

(defn close [force?]
  (when force?
    (object/raise app :closed))
  (.close win force?))

(defn refresh []
  (js/window.location.reload true))

(defn open-window []
  (let [id (swap! js/global.windowsId inc)
        w (.Window.open gui "index.html" (clj->js {:toolbar false
                                                   :show false}))]
    (set! (.-ltid w) id)
    (swap! windows assoc id w)))

(defn ready? [this]
  (= 0 (:delays @this)))


(object/behavior* ::ready
                  :triggers #{:ready}
                  :reaction (fn [this]
                              (log "app ready")
                              (nw/show)))

(object/behavior* ::quit
                  :triggers #{:quit}
                  :reaction (fn [this]
                              (log "Quitting...")
                              (nw/quit)))


(object/object* ::app
                :tags #{:app}
                :triggers [:quit :ready]
                :behaviors [::quit ::ready]
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
(def note-editor (object/create :think.objects.note-editor/note-editor))

(defn setup-tray
  []
  "Creates a tray menu in upper-right app tray."
  (log "creating tray menu...")
  (nw/tray! {:title "Thinker"
             :menu (nw/menu [{:label "Take note..." :onclick (object/raise note-editor :take-note)}
                             {:label "Show"         :onclick #(.show (nw/window))}
                             {:type "separator"}
                             {:label "Quit"         :onclick #(object/raise app :quit)}])}))


(defn load-document
  [doc-id]
  (let-realised [doc (model/get-document doc-id)]
    (object/raise workspace/workspace :load-document @doc)))


(defn load-home
  []
  (load-document :home))

(defn init []
  (log "Starting app...")
  (think.util/start-repl-server)
  (object/raise app :init-home)
  ; (setup-tray)  ;; this is creating a new tray icon everytime i refresh and keeping the old ones
  )


(react-to #{:db-loaded} load-home)
