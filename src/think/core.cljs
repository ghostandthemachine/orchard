(ns think.core
  (:use        [think.log          :only (log log-obj)])
  (:use-macros [dommy.macros       :only (sel)])
  (:require [clojure.browser.repl  :as repl]
            [clojure.string        :as string]
            [node-webkit.core      :as nw]
            [dommy.core            :as dom]
            [dommy.template        :as dt]
            [think.clou            :as clou]
            [think.util            :as util]
            [think.model           :as model]))


(def ^:private os      (js/require "os"))
;(def ^:private process (js/require "process"))

(def APP "Thinker")

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
                (with-tabs "main"
                  [["Drop Zone" (drop-zone-view)]
                   ["Clou" (clou/view)]]))]
    (dom/replace! body elem)))


(defn init
  []
  (let [argv (nw/argv)]
    ; (log "args: " argv)
    (repl/connect "http://127.0.0.1:9000/repl"))

  (nw/menu [{:label "Testing..."}
            {:label "Foobar"}])


  ;(setup-tray)
 
  ; Quit on window close
  (.on (nw/window) "close" nw/quit)
  ;(test-db db)

  (init-view)
  (clou/init-code-mirror)

  (js/setTimeout (fn [] (.focus js/window)) 1000)

  (util/setup-drop-zone :#drop-spot)

  (.show (nw/window))
  (.focus (nw/window))
  (log "Location: " (.-location js/window)))

