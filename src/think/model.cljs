(ns think.model
  (:refer-clojure  :exclude [create-node])
  (:use [think.log :only    (log log-obj)])
  (:require [think.util         :as util]
            [think.dispatch     :as dispatch]
            [think.db           :as db]
            [redlobster.promise :as p]))


(defn project
  [title]
  {:type ::project
   :id      (util/uuid)
   :title   title
   :updated (util/date-json)})


(defn wiki-page
  [project]
  {:type ::wiki-page
   :id      (util/uuid)
   :project project
   :modules []
   :updated (util/date-json)
   })


(defn wiki-module
  []
  {:type ::wiki-module
   :id    (util/uuid)
   :title "title"
   :body  "body"
   })


(defn document
  []
  {:type ::document
   :id          (util/uuid)
   :title       "title"
   :authors     []
   :path        "path"
   :filename    "filename.pdf"
   :notes       []
   :annotations []
   :cites       []
   :tags        []
   })


(def DB-PATH "data/project.db")

(def project-db* (atom nil))

(defn init-project-db
  []
  (when (nil? @project-db*)
    (let [db-promise (db/open DB-PATH)]
      (p/on-realised db-promise
        #(reset! project-db* %)
        (fn [err]
          (log "Error opening DB!")
          (log-obj err))))))


(defn create-project
  [title]
  (let [proj (project title)]
    (db/create-doc @project-db* (clj->js proj))))


(defn delete-project
  [proj]
  (db/delete-doc @project-db* (clj->js proj)))


(defn all-projects
  []
  (db/all-docs @project-db*))





