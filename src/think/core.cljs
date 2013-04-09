(ns think.core
  (:use [think.util :only [ready log log-obj refresh r! clipboard read-clipboard open-window editor-window]])
  (:use-macros [dommy.macros :only [sel]])
  (:require [clojure.browser.repl :as repl]
            [clojure.string :as string]
            [node-webkit.core :as nw]
            [dommy.core :as dom]
            [dommy.template :as dt]
            [think.clou :as clou]
            [think.pdf :as pdf]
            [think.view-helpers :refer [with-layout with-tabs drop-zone-view wrap generate-id-from-name]]
            [think.graph-view :as graph]
            [think.webrtc :as webrtc]))


(def ^:private os (js/require "os"))

(def APP "Thinker")

(defn init-view
  "Takes a dommy (Hiccup) type vector of html elements then appends them to body."
  [dommy-structure]
  (let [body (first (sel "body"))]
    (dom/append! body (dt/node dommy-structure))))

; (pdf/setup-pdf-drop-zone :#main-pdf-viewer)


(def ns-table
  {"webrtc" think.webrtc/init})

(defn location-init-fn
  []
  (let [abs      (.-href (.-location js/document))
        relative (re-find #"/\w+.html" abs)
        ns-str   (apply str (rest (first (clojure.string/split relative ".html"))))]
    (ns-str ns-table)))


(defn init
  []

  ; (nw/menu [{:label "Testing..."}
            ; {:label "Foobar"}])
  ; (setup-tray)
  ; Quit on window close
  (.on (nw/window) "close" nw/quit)
  (.show (nw/window))
  (.focus (nw/window))
  (repl/connect "http://127.0.0.1:9000/repl")
  (js/setTimeout (fn [] (.focus js/window)) 1000)

  (init-view (webrtc/view))
  (webrtc/init))


(ready init)
