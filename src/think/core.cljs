(ns think.core
  (:use [think.util :only [log log-obj]])
  (:use-macros [dommy.macros :only [sel]])
  (:require [clojure.browser.repl :as repl]
            [clojure.string :as string]
            [jayq.core :as jq]
            [node-webkit.core :as nw]
            [dommy.core :as dom]
            [dommy.template :as dt]
            [think.clou :as clou]
            [think.pdf :as pdf]
            [think.view-helpers :as view]
            [think.graph-view :as graph]))


(def ^:private os      (js/require "os"))

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
  (util/open-window "editor.html"))


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
                (view/with-tabs "main"
                  [["Drop Zone" (pdf/main-pdf-viewer)]
                   ["Clou"      (clou/view)]
                   ["Graph"     (graph/view)]]))]
    (dom/replace! body elem)
    (pdf/setup-pdf-drop-zone :#main-pdf-viewer)
    ))


(defn init
  []
  (let [argv (nw/argv)]
    ; (log "args: " argv)
    (repl/connect "http://127.0.0.1:9000/repl"))

  (nw/menu [{:label "Testing..."}
            {:label "Foobar"}])


  (setup-tray)

  ; Quit on window close
  (.on (nw/window) "close" nw/quit)
  ;(test-db db)

  (init-view)
  (clou/init-code-mirror)
  (graph/init-graph-view)

  (js/setTimeout (fn [] (.focus js/window)) 1000)

  (util/setup-drop-zone :#drop-spot)

  (.show (nw/window))
  (.focus (nw/window))
  (log "Location: " (.-location js/window)))

