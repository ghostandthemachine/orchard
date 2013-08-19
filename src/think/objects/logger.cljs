(ns think.objects.logger
  (:require-macros 
    [think.macros :refer [defui defgui defonce]])
  (:require [think.object :as object]
            [think.util.log :refer (log log-obj)]
            [think.util.dom :as dom]
            [think.util.core :as util]
            [think.dispatch :as dispatch]
            [crate.core :as crate]))

(def ready* (atom false))
(defn ready? [] @ready*)

(defn tab-content
  [id]
  (dom/$ (str "#" id)))


(defn tab-ul
  [id]
  (dom/$ (str "#" id " ul")))


(defn tab-elem
  [id]
  (dom/$ (str "li a[href='" id "']")))


(defn scroll-to
  ([elem pos]
    (scroll-to elem pos 1))
  ([elem pos dur]
    (.animate (js/$ elem)
      (clj->js
        {:scrollTop pos})
      dur)))

(defn scroll-to-top-of
  [elem1 elem2]
  (scroll-to elem1
    (.-top (.offset (js/$ elem2)))))

(defn scroll-to-end
  [elem]
  (scroll-to elem
    (.-scrollHeight elem)))

(def MAX-DIFF 10)


(defn append-message
  [log-id msg]
  (when (ready?)
    (let [tab     (tab-ul log-id)
          parent  (dom/$ (str "#" log-id))
          ptop    (.-scrollTop parent)
          pheight (- (.-scrollHeight parent) 200) ;; remove 200px padding???
          diff    (- ptop pheight)
          scroll? (< diff MAX-DIFF)]
      (.log js/console "scrollHeight " pheight " top " ptop)
      (dom/append tab
        (crate/html
          [:li.log-row
            [:p (str msg)]]))
      (when scroll?
        (scroll-to-end parent)))))


(defgui tab
  [id label & args]
  [:li
   (if (= :active (first args))
     [:a.active {:href id :data-toggle "tab"} label]
     [:a {:href id :data-toggle "tab"} label])])


(defui tabs
  []
  [:div.loggger-element
   [:div.tabs-container
    [:ul#logger-tabs.nav.nav-tabs
     (tab "#log" "Log" :active)
     (tab "#couchdb" "Couchdb")]]
   [:div.tab-content.logger-content-panes
    [:div#log.tab-pane.log-pane.active
     [:ul.log-list]]
    [:div#couchdb.tab-pane.log-pane
     [:ul.log-list]]]])


(defn post
  [type msg]
  (append-message (name type) msg))


(object/object* :logger
  :tags #{:logger}
  :triggers []
  :behaviors []
  :delays 0
  :init (fn [this]
          (tabs)))


(def logger (object/create :logger))


(defn ready
  []
  (let [log-panes (js/$ js/document ".log-pane")]
    (for [i (.size log-panes)]
      (let [elem       (.get log-panes i)
            height     (.-scrollHeight elem)
            cur-scroll (.-scrollTop elem)]))
    (dom/append
      (dom/$ "#logger")
      (:content @logger))
    (.tab (js/$ "#logger-tabs a:last") "show")
    (reset! ready* true)))



(dispatch/react-to #{:log-message}
                   (fn [ev & [data]]
                     (post :log data)))


(defn toggle
  [b]
  (dom/css (dom/$ "#logger") {:visibility (if b "visible" "hidden")}))


(defn show-logger [] (toggle true))
(defn hide-logger [] (toggle false))
