(ns think.view-helpers
  (:require [clojure.string :as string]))

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
