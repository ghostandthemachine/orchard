(ns think.core
  (:use-macros [dommy.core-compile :only [sel]])
  (:require [clojure.browser.repl :as repl]
            [clojure.string :as string]
            [jayq.core :as jq]
            [node-webkit.core :as nw]
            [dommy.core :as dom]
            [dommy.template :as dt]
            [think.clou :as clou]))


(def ^:private gui     (js/require "nw.gui"))
(def ^:private os      (js/require "os"))
(def ^:private fs      (js/require "fs"))

;(def ^:private process (js/require "process"))

(def APP "Thinker")


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
                   (js/alert "path: " (.-path file))
                 (try
                   (.Shell.showItemInFolder gui (.-path file))
                   (catch js/Error e
                     (log "Got an error: " e))
                   (catch js/global.Error e
                     (log "Got a global error: " e)))
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


(defn generate-id-from-name
  [n end]
  (str (string/lower-case (string/replace n #" " "-")) end))


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

(defn drop-zone-view
  []
  (with-layout
    [:div.row-fluid
      [:h2.unselectable "Thinker"]]
    [:div.row-fluid
      [:div#interface]]
    [:div.unselectable.row-fluid
      [:div#drop-spot
        "Drop file here"]]))

(defn with-tabs
  "Creates a bootstrap tabbed component. Expects a seq of vectors in the form [label html-element(hiccup format not js node)]."
  [id elements]
  [:div {:id (str id "-tab-container")}
    [:ul.nav.nav-tabs {:id (str id "-tab-nav")}
      (let [label (-> elements first first)]
        [:li.unselectable
          [:a.unselectable {:href (str "#" (generate-id-from-name label "-tab-content")) :data-toggle "tab"} label]])
      (for [label (map first (rest elements))]
        [:li.unselectable
          [:a.unselectable {:href (str "#" (generate-id-from-name label "-tab-content")) :data-toggle "tab"} label]])]
    [:div.tab-content
      (let [[label element] (first elements)]
        ;; first tab MUST be activated or no tabs will show.
        [:div.tab-pane.active {:id (generate-id-from-name label "-tab-content")}
          element])
      (for [[label content] (rest elements)]
        [:div.tab-pane {:id (generate-id-from-name label "-tab-content")} content])]])

(defn init-view
  []
  (let [body (first (sel "#main-content"))
        elem (dt/node
                (with-tabs "main"
                  [["Drop Zone" (drop-zone-view)]
                   ["Clou" (clou/view)]]))]
    (dom/replace! body elem)))

(defn init []
  (let [argv (nw/argv)]
    ; (log "args: " argv)
    (repl/connect "http://127.0.0.1:9000/repl")
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
  (clou/init-code-mirror)

  ; Write out a text file using the node.js fs namespace...
  ;(.writeFile fs "foo.txt" "This is a test...")
  (js/setTimeout (fn [] (.focus js/window)) 1000)

  (setup-drop-zone :#drop-spot)

  (.show (nw/window))
  ; (.focus (nw/window))
  (log "Location: " (.-location js/window))
  (js/alert "asdf")
)

(jq/document-ready init)
