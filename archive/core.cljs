(ns think.core
  ; (:use [think.util :only [start-repl-server ready log log-obj refresh r! clipboard read-clipboard open-window editor-window]])
  ; (:use-macros [dommy.macros :only [sel]])
  ; (:require [clojure.string :as string]
  ;           [node-webkit.core :as nw]
  ;           [dommy.core :as dom]
  ;           [dommy.template :as dt]
  ;           [think.editor :as editor]
  ;           [think.view-helpers :refer [with-layout with-tabs drop-zone-view wrap generate-id-from-name]]
  ;           [think.graph-view :as graph]
  ;           [think.webrtc :as webrtc]
  ;           [think.app :as app]
  ;           [think.project :as project])
  )


; (def ^:private os (js/require "os"))

; (def APP "Thinker")

; ; (pdf/setup-pdf-drop-zone :#main-pdf-viewer)


; (def ns-table
;   {"webrtc" think.webrtc/init})

; (defn location-init-fn
;   []
;   (let [abs      (.-href (.-location js/document))
;         relative (re-find #"/\w+.html" abs)
;         ns-str   (apply str (rest (first (clojure.string/split relative ".html"))))]
;     (ns-str ns-table)))


; (defn init
;   []

;   ; (nw/menu [{:label "Testing..."}
;             ; {:label "Foobar"}])
;   ; (setup-tray)
;   ; Quit on window close
;   (.on (nw/window) "close" nw/quit)
;   (.show (nw/window))
;   (.focus (nw/window))
;   (start-repl-server))

; (ready init)


