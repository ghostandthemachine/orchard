(ns think.model
  (:use [think.log :only (log log-obj)])
  (:require [think.util :as util]))


(def think-db* (atom nil))

(defn project
  [title]
  {:type ::project
   :id (util/uuid)
   :title title
   :updated (util/date-json)})


(defn wiki-page
  [project]
  {:type ::wiki-page
   :id (util/uuid)
   :project project
   :modules []
   :updated (util/date-json)})


(defn wiki-module
  []
  {:type ::wiki-module
   :id (util/uuid)
   :title "title"
   :body  "body"
   })


(defn document
  []
  {:type ::document
   :id (util/uuid)
   :title "title"
   :authors []
   :path "path"
   :filename "filename.pdf"
   :notes []
   :annotations []
   :cites []
   :tags []
   })

(defn test-db
  [db]
  (.post db (clj->js {"foo" 2 "bar" "asdf"})
         (fn [err resp]
           (log "post err: " err)
           (log "post resp: " resp))))
