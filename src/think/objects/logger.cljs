(ns think.objects.logger
  (:use-macros [redlobster.macros :only [let-realised]]
  						 [think.macros :only [defui defgui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util.dom  :as dom]
            [think.util :as util]
            [think.dispatch :as dispatch]
            [crate.core :as crate]
            [redlobster.promise :as p]))

(declare logger-win)

(when-not logger-win
	(def logger-win (.open js/window
			"http://localhost:3000/logger.html")))


; (when-not (think.kv-store/local-get :logger-open?)
;   (def logger-win (.open js/window
;       "http://localhost:3000/logger.html"))
;   (think.kv-store/local-set :logger-open? true))


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
	(.get (.find (body$) id) 0))


(defn tab-elem
	[id]
	(.find (body$) (str "li a[href='" id "']")))


(defn append-message
	[log-id msg & args]
	(.append (tab-content$ log-id)
		(crate/html
			[:li.log-row
				[:p (str msg args)]])))


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
				(tab "#couchdb" "Couchdb")
				(tab "#cljsbuild" "Cljsbuild")]]
			[:div.tab-content.logger-content-panes
  			[:div.tab-pane.log-pane.active {:id "log"}
  				[:ul.log-list]]
  			[:div.tab-pane.log-pane  {:id "couchdb"}
  				[:ul.log-list]]
  			[:div.tab-pane.log-pane {:id "cljsbuild"}
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
                    (.log js/console "scroll data " height cur-scroll)
                    (if (= height cur-scroll)
                      (do
                        (append-message log-type msg)
                        (set! (.-scrollTop elem) height))
                      (append-message log-type msg)))
                  (append-message log-type msg)))))


(object/behavior* ::ready
  :triggers #{:ready}
  :reaction (fn [this]
  						(log "logger inint content")
              (let [log-panes (.find log-doc ".log-pane")]
    						(for [i (.size log-panes)]
                  (let [elem       (.get log-panes i)
                        height     (.-scrollHeight elem)
                        cur-scroll (.-scrollTop elem)]
                    (set! (.-scrollTop elem) height)
                    (log "scrollTop for elem " height)))
                (dom/append
                	(body)
                	(:content @this))
                (.tab (js/$ "#logger-tabs a:last") "show"))))



(object/behavior* ::quit
  :triggers #{:quit}
  :reaction (fn [this]
              (log "Closing Logger")
              (nw/quit)))


(object/behavior* ::show-dev-tools
                  :triggers #{:show-dev-tools}
                  :reaction (fn [this]
                              (log "Show Dev Tools")
                              (.showDevTools logger-win)))


(object/object* :logger
                :tags #{:logger}
                :triggers [:quit :ready :show-dev-tools :init-window :post]
                :behaviors [::quit ::ready ::show-dev-tools ::init-window ::post-message]
                :delays 0
                :init (fn [this]
                        (log "init logger")
                        (logger-content)))


(def logger (object/create :logger))



(dispatch/react-to #{:log-message}
  (fn [ev & [data]]
    (object/raise logger :post :log data)))
