(ns think.objects.app
  (:require [think.object :as object]
            [think.objects.context :as ctx]
            [think.util.js :refer [now]]
            [think.util.log :refer [log]]
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

(object/behavior* ::refresh
                  :triggers #{:refresh}
                  :reaction (fn [obj]
                              (set! closing true)
                              (object/raise app :reload)
                              (when closing
                                (refresh))))

(object/behavior* ::close!
                  :triggers #{:close!}
                  :reaction (fn [this]
                              (set! closing true)
                              (object/raise this :close)
                              (when closing
                                (object/raise this :closed)
                                (close true))))

(object/behavior* ::show!
                  :triggers #{:show!}
                  :reaction (fn [this]
                              (.show win)
                              (object/raise app :show)))

(object/behavior* ::delay!
                  :triggers #{:delay!}
                  :reaction (fn [this]
                              (object/update! this [:delays] inc)))

(object/behavior* ::store-position-on-close
                  :triggers #{:closed}
                  :reaction (fn [this]
                              (set! js/localStorage.x (.-x win))
                              (set! js/localStorage.y (.-y win))
                              (set! js/localStorage.width (.-width win))
                              (set! js/localStorage.height (.-height win))
                              (set! js/localStorage.fullscreen (.-isFullscreen win))
                              ))

(object/behavior* ::restore-fullscreen
                  :triggers #{:show}
                  :reaction (fn [this]
                                (when (= js/localStorage.fullscreen "true")
                                  (.enterFullscreen win))))

(object/behavior* ::restore-position-on-init
                  :triggers #{:init}
                  :reaction (fn [this]
                              (when (.-width js/localStorage)
                                (.resizeTo win (js/parseInt js/localStorage.width) (js/parseInt js/localStorage.height))
                                (.moveTo win (js/parseInt js/localStorage.x) (js/parseInt js/localStorage.y)))))

(object/behavior* ::ready!
                  :triggers #{:delay!}
                  :reaction (fn [this]
                              (object/update! this [:delays] dec)
                              (ready? this)))

; (object/behavior* ::on-show-bind-navigate
;                   :triggers #{:show}
;                   :reaction (fn [this]
;                               (dom/on ($ js/document :#canvas) :click (fn [e]
;                                                             ;;TODO: when prevent default has been called don't do this.
;                                                             (when (= (.-target.nodeName e) "A")
;                                                               (dom/prevent e)
;                                                               (when-let [href (.-target.href e)]
;                                                                 (.Shell.openExternal gui href)
;                                                                 (.focus win)))))))

(object/behavior* ::startup-time
                  :triggers #{:show}
                  :reaction (fn [this]
                              (- (now) js/setup.startTime))
                  )

(object/object* ::app
                :tags #{:app}
                :trigers [:init :close :reload :refresh :close!]
                :behaviors [::refresh ::close! ::show! ::delay! ::ready! ::startup-time
                            ::on-show-bind-navigate]
                :delays 0
                :init (fn [this]
                        (ctx/in! :app this)
                        ))

(object/tag-behaviors :app [::store-position-on-close ::restore-position-on-init ::restore-fullscreen])

(when-not js/global.windows
  (set! js/global.windows (atom (sorted-map 0 win)))
  (set! js/global.windowsId (atom 0))
  (set! (.-ltid win) 0))

(def windows js/global.windows)
(def app (object/create ::app))


(defn init []
  (log "Starting app...")
  (think.util/start-repl-server)
  ; (object/raise app :deploy)
  ; (object/raise app :pre-init)
  ; (object/raise app :init)
  ; (object/raise app :post-init)
  (object/raise app :show!)
  )

