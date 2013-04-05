(ns think.wiki-scrapper
  (:use-macros [jayq.macros :only [let-ajax]]
               [dommy.macros :only [sel]])
  (:require [goog.net.XhrIo :as xhr]
            [clojure.string :as string]
            [goog.events :as events]
            [jayq.core :as jq]
            [goog.Uri.QueryData :as query-data]
            [goog.structs :as structs]
            [dommy.core :as dom]
            [dommy.template :as dt]
            [think.util :refer [log]]))

(def test-route "http://en.wikipedia.org/wiki/Directed_graph")

(defn ->method [m]
  (string/upper-case (name m)))

(defn parse-route [route]
  (cond
    (string? route) ["GET" route]
    (vector? route) (let [[m u] route]
                      [(->method m) u])
    :else ["GET" route]))

(defn ->data [d]
  (let [cur (clj->js d)
        query (query-data/createFromMap (structs/Map. cur))]
    (str query)))

(defn ->callback [callback]
  (when callback
    (fn [req]
      (let [data (. req (getResponseText))]
        (callback data)))))

(defn xhr [route content callback & [opts]]
  (let [req (new goog.net.XhrIo)
        [method uri] (parse-route route)
        data (->data content)
        callback (->callback callback)]
    ; (when callback
    ;   (events/listen req goog.net.EventType/COMPLETE #(callback req)))
    ; (. req (send uri method data (when opts (clj->js opts))))
    (.send req uri callback)
    ))



(def nodes (atom nil))

(defn grab-page
  []
  (let-ajax [html {:url test-route
                   :dataType :text}]
    (reset! nodes (dt/html->nodes html))))

(defn get-content
  [elements]
  (first
    (filter
      #(= (.-id %) "content")
      elements)))

(defn get-ciatitions
  [content]
  (sel content :.citation))


(grab-page)

(def citations (get-ciatitions (get-content @nodes)))

(doseq [c citations] (println (.-data (.-firstChild c))))


  (println (sel (get-content @nodes) :.citation))




