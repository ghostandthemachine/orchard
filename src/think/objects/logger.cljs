(ns think.objects.logger
  (:require-macros [redlobster.macros :refer [let-realised]]
  						     [think.macros :refer [defui defgui defonce]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util.dom  :as dom]
            [think.util.core :as util]
            [think.dispatch :as dispatch]
            [crate.core :as crate]
            [redlobster.promise :as p]))


(defonce logger-win (.open js/window "http://localhost:3000/logger.html", "Logger", "left=0, top=0"))


(defn log-doc
  []
  (.-document logger-win))


(defn body
  []
  (.getElementById (log-doc) "logger"))

(defn body$ [] (js/$ (body)))


(defn tab-content
  [id]
  (.getElementById (log-doc) id))


(defn tab-content$
  [id]
  (.find (js/$ (log-doc)) (str "#" id " ul")))


(defn by-id
  [id]
  (.get (.find (body$) (str "#" id)) 0))


(defn tab-elem
  [id]
  (.find (body$) (str "li a[href='" id "']")))


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


(defn append-message
  [log-id msg & args]
  (let [tab (tab-content$ log-id)]
    (.append tab (crate/html [:li.log-row [:p (str msg args)]]))
    (set! (.-scrollTop tab) (.-scrollHeight tab))))


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
    [:div.tab-pane.log-pane.active {:id "log"}
     [:ul.log-list]]
    [:div.tab-pane.log-pane  {:id "couchdb"}
     [:ul.log-list]]]])


(defui logger-content
  []
  [:div.row-fluid
   (tabs)])


(object/behavior* ::post-message
  :triggers #{:post}
  :reaction (fn [this type msg & args]
              (let [log-type   (str (name type))]
                (if-let [elem       (tab-content log-type)]
                  (let [height     (.-scrollHeight elem)
                        cur-scroll (.-scrollTop elem)]
                    (if (= height cur-scroll)
                      (do
                        (append-message log-type msg)
                        (set! (.-scrollTop elem) height))
                      (append-message log-type msg)))
                  (append-message log-type msg)))))


(defn ready
  [this]
  (let [log-panes (.find (js/$ (log-doc)) ".log-pane")]
    (for [i (.size log-panes)]
      (let [elem       (.get log-panes i)
            height     (.-scrollHeight elem)
            cur-scroll (.-scrollTop elem)]
        ; (log "set log pane top " height cur-scroll)
        (set! (.-scrollTop elem) height)
        ;(log "scrollTop for elem " height)
        (.on logger-win "close"
          (fn []
            (this-as this (.close this true))))
        ))
    (dom/append
      (body)
      (:content @this))
    (.tab (js/$ "#logger-tabs a:last") "show")))


(object/behavior* ::quit
  :triggers #{:quit}
  :reaction (fn [this]
              (log "Closing Logger")
              (.close logger-win)))


(defn show-dev-tools
  []
  (log "Show Logger Dev Tools")
  (.showDevTools logger-win))


(object/object* :logger
  :tags #{:logger}
  :triggers [:quit :show-dev-tools :init-window :post]
  :behaviors [::quit ::show-dev-tools ::init-window ::post-message]
  :delays 0
  :init (fn [this]
          (logger-content)))


(def logger (object/create :logger))

(aset logger-win "onload" (partial ready logger))

(dispatch/react-to #{:log-message}
                   (fn [ev & [data]]
                     (object/raise logger :post :log data)))


