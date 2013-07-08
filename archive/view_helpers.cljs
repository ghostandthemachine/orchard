(ns think.view-helpers
  (:use-macros [dommy.macros :only [sel]])
  (:require [clojure.string :as string]
            [dommy.core :as dom]
            [dommy.template :as dt]))

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

(defn append-body
  "Takes a dommy (Hiccup) type vector of html  elements then appends them to body."
  [dommy-structure]
  (let [body (first (sel "body"))]
    (dom/append! body (dt/node dommy-structure))))