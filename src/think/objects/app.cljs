(ns think.objects.app
  (:require [think.object :as object]
            [think.objects.context :as ctx]
            [think.util.js :refer [now]]
            [think.util.log :refer [log]]
            [think.model :as model]
            [think.util.dom :refer [$ html append] :as dom]))

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

(defn args []
  (seq (.-App.argv gui)))




; (object/behavior* ::load-home
;                   :triggers #{:init}
;                   :reaction (fn [this]
;                               (log "init and raise init-db")))


(object/object* ::app
                :tags #{:app}
                :trigers [:init]
                :behaviors [::load-home]
                :delays 0
                :init (fn [this]
                        (ctx/in! :app this)
                        ))

; (object/tag-behaviors :app [::store-position-on-close ::restore-position-on-init ::restore-fullscreen])

(when-not js/global.windows
  (set! js/global.windows (atom (sorted-map 0 win)))
  (set! js/global.windowsId (atom 0))
  (set! (.-ltid win) 0))

(def windows js/global.windows)
(def app (object/create ::app))


(defn init []
  (log "Starting app...")
  (think.util/start-repl-server)
  (object/raise app :init))

