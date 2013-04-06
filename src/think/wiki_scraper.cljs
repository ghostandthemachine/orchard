(ns think.wiki-scrapper
  (:use-macros [dommy.macros :only [sel]]
               [redlobster.macros :only [promise]])
  (:require [dommy.core :as dom]
            [dommy.template :as dt]
            [think.util :refer [log]]
            [think.model :as model]
            [think.xhr :refer [xhr xhr-promise]]
            [redlobster.promise :as p]))

(defn wiki-url [term] (str "http://en.wikipedia.org/wiki/" term))

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
;       citations)))


(defn get-page-promise
  [search-term]
  (xhr-promise (wiki-url search-term) {}))


(comment

  (def page-promise (get-page-promise "directed_graph"))

  (p/on-realised page-promise
    #(println "Succeeded" %)
    #(println "Error"))




  )