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
            [think.util :refer [log]]
            [think.model :as model]))

; (defn load-page-citations
;   [search-term]
;   (let-ajax [html {:url (str "http://en.wikipedia.org/wiki/" search-term)
;                    :dataType :text}]
;     (let [html-nodes  (dt/html->nodes html)
;           citations (->
;                       (first
;                         (filter
;                           #(= (.-id %) "content")
;                           html-nodes))
;                       (sel :.citations))
;           parent-node (model/create-node {:name search-term :color :green})]
;       (doseq [c citations]
;         (let [child (model/create-node {:name (.-data (.-firstChild c))})]
;           (log child)
;           (model/create-edge parent-node child
;             {:name (.-data (.-firstChild c))})))
;       content)))




