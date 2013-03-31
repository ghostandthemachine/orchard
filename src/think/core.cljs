(ns think.core
  (:use-macros [dommy.core-compile :only [sel]])
  (:require [clojure.browser.repl :as repl]
            [jayq.core :as jq]
            [node-webkit.core :as nw]
            [dommy.core :as dom]
            [dommy.template :as dt]))


(def ^:private gui     (js/require "nw.gui"))
(def ^:private os      (js/require "os"))
(def ^:private fs      (js/require "fs"))

;(def ^:private process (js/require "process"))


(defn wrap
  ([parent elem]
    (conj parent elem))
  ([parent elem & elements]
  (reduce
    (fn [p n] (conj p n))
    (conj parent elem)
    elements)))

(defn with-layout
  ([elem]
    [:div.container-fluid
      (wrap [:div.row-fluid] elem)])
  ([elem & elems]
  [:div.container-fluid {:id "main-content"}
    (wrap [:div.row-fluid]
      elem
      elems)]))

(defn drop-view
  []
  (with-layout
    [:div.row-fluid
      [:h2 "Thinker"]]
    [:div.row-fluid
      [:div#interface]]
    [:div.row-fluid
      [:div#drop-spot
        "Drop file here"]]))


(def APP "Thinker")

; (defn log [& args]
;   (.log js/console (apply pr-str args)))

(defn log [v & text]
  (let [vs (if (string? v)
             (apply str v text)
             v)]
    (. js/console (log vs))))

(defn log-obj [obj]
  (.log js/console obj))

(defn clipboard [] (.get js/Clipboard))
(defn read-clipboard [] (.get (clipboard)))

(defn file-drop
  [elem handler]
  (set! (.-ondragover js/window) (fn [e] (.preventDefault e) false))
  (set! (.-ondrop js/window) (fn [e] (.preventDefault e) false))

  (-> (jq/$ elem)
      (.ondragover (fn []
                     (this-as spot
                              (set! (.-className spot) "hover"))
                     false))

      (.ondragend (fn []
                    (this-as spot
                             (set! (.-className spot) ""))))

      (.ondrop (fn [e]
                 (.preventDefault e)
                 (println "drop testing...")
                 (doseq [file (.-files (.-dataTransfer e))]
                   (log "File path: " (.-path file)))
                 false))

      ))


(defn handle-file-select [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (let [files (.-files (.-dataTransfer evt))]
    (dotimes [i (.-length files)]
      (let [rdr (js/FileReader.)
            the-file (aget files i)]
        (log "File path: " (.-path the-file))
        (comment set! (.-onload rdr)
              (fn [e]
                (let [file-content (.-result (.-target e))
                      file-name (if (= ";;; " (.substr file-content 0 4))
                                  (let [idx (.indexOf file-content "\n\n")]
                                    (.log js/console idx)
                                    (.slice file-content 4 idx))
                                  (.-name the-file))]
                  (.log js/console (str "file-name " file-name))
                  (.set storage file-name file-content)
                  (swap! list-of-code conj file-name))))
        (.readAsText rdr the-file)))))

(defn handle-drag-over [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (set! (.-dropEffect (.-dataTransfer evt)) "copy"))

(defn setup-drop-zone
  [id]
  (let [dz (first (sel id))]
    (dom/listen! dz :dragover handle-drag-over)
    (dom/listen! dz :drop     handle-file-select)))

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
        elem (dt/node (drop-view))]
    (dom/replace! body elem)))

(defn init []
  (let [argv (nw/argv)]
    ; (log "args: " argv)
    (repl/connect "http://localhost:9000/repl")
    )

  (nw/menu [{:label "Testing..."}
            {:label "Foobar"}])


  (setup-tray)
  ;(file-drop "drop-spot" nil)

  ; Quit on window close
  (.on (nw/window) "close" nw/quit)

  ;(jq/append (jq/$ :#container) "Ready!")

  ;(-> (jq/$ :#interface)
  ;    (jq/css {:background "blue"})
  ;    (jq/inner "Loading…")))

  (init-view)

  ; Write out a text file using the node.js fs namespace...
  ;(.writeFile fs "foo.txt" "This is a test...")
  (js/setTimeout (fn [] (.focus js/window)) 1000)

  (setup-drop-zone :#drop-spot)

  (.show (nw/window))
  (.focus (nw/window))
)

(jq/document-ready init)
