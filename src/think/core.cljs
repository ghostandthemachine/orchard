(ns think.core
  (:use [think.util :only [log]])
  (:use-macros [dommy.core-compile :only [sel]])
  (:require [clojure.browser.repl :as repl]
            [clojure.string :as string]
            [jayq.core :as jq]
            [node-webkit.core :as nw]
            [dommy.core :as dom]
            [dommy.template :as dt]
            ; [think.clou :as clou]
            ; [think.view-helpers :as view]
            ; [think.pdf :as pdf]
            ))

(def ^:private process (js/require "process"))

(def APP "Thinker")


(defn refresh []
  (js/window.location.reload true))

(defn r! []
  (refresh))

(defn log-obj [obj]
  (.log js/console obj))

(defn clipboard [] (.get js/Clipboard))
(defn read-clipboard [] (.get (clipboard)))

(defn open-window
  [url & {:as options}]
  (let [o (merge {:x 0 :y 0 :width 400 :height 600} options)
        opt-str (format "screenX=%d,screenY=%d,width=%d,height=%d"
                        (:x o) (:y o) (:width o) (:height o))]
    (.open js/window url nil opt-str)))


(defn editor-window
  []
  (open-window "editor.html"))


(defn setup-tray []
  "Creates a tray menu in upper-right app tray."
 (nw/tray! {:title APP
             :menu (nw/menu [{:label "Show" :click #(.show (nw/window))}
                             ;{:label "Close all" :click #(.App.closeAllWindows)}
                             {:type "separator"}
                             {:label "Editor..." :click editor-window}
                             {:label "Quit" :click nw/quit}
                             ])}))

(defn init-view
  []
  (let [body (first (sel "#main-content"))
        elem (dt/node
                (view/with-tabs "main"
                  [["Drop Zone" (pdf/main-pdf-viewer)]
                   ["Clou" (clou/view)]]))]
    (dom/replace! body elem)
    (pdf/setup-pdf-drop-zone :#main-pdf-viewer)))

(defn init []
  (let [argv (nw/argv)]
    ; (log "args: " argv)
    (repl/connect "http://127.0.0.1:9000/repl")
    )

  (nw/menu [{:label "Testing..."}
            {:label "Foobar"}])


  ;(setup-tray)
  ;(file-drop "drop-spot" nil)

  ; Quit on window close
  (.on (nw/window) "close" nw/quit)

  ;(jq/append (jq/$ :#container) "Ready!")

  ;(-> (jq/$ :#interface)
  ;    (jq/css {:background "blue"})
  ;    (jq/inner "Loading…")))

  ; (init-view)
  ; (clou/init-code-mirror)

  ; Write out a text file using the node.js fs namespace...
  ;(.writeFile fs "foo.txt" "This is a test...")
  (js/setTimeout (fn [] (.focus js/window)) 1000)

  (.show (nw/window))
  ; (.focus (nw/window))
  (log "Location: " (.-location js/window)))


; (jq/document-ready init)
