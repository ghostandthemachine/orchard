(ns think.model
  (:refer-clojure  :exclude [create-node])
  (:use [think.log :only    (log log-obj)])
  (:use-macros [redlobster.macros :only [let-realised defer-node]])
  (:require [think.util         :as util]
            [think.dispatch     :refer [fire react-to]]
            [think.db           :as db]
            [redlobster.promise :as p]))


(def DB-PATH "data/document.db")

(def document-db* (atom nil))

(defmulti doc->record     :type
  "Convert a pouchDB document to a think document record.")

(defmulti create-document :type
  "Create a document given a map.")


(defn init-document-db
  []
  (when (nil? @document-db*)
    (let [db-promise (db/open DB-PATH)]
      (p/on-realised db-promise
        #(do
           (reset! document-db* %)
           (fire :db-ready @document-db*))
        (fn [err]
          (log "Error opening DB!")
          (log-obj err))))))


(defrecord PDFDocument
  [type id rev created-at updated-at
   title authors path filename notes annotations cites tags])

(defrecord WikiDocument
  [id rev created-at updated-at
   template title modules])

(defmethod doc->record ::wiki-document
  [{:keys [id rev template title modules created-at updated-at]}]
  (WikiDocument. id rev template title modules created-at updated-at))

(defmethod create-document ::wiki-document
  [{:keys [template title]}]
  (WikiDocument. (util/uuid) nil template title [nil] (util/date-json) (util/date-json)))


(defrecord SingleColumnTemplate [modules])

(defrecord MarkdownModule [text])
(defrecord FormModule     [fields])
(defrecord QueryModule    [query template])


(defn delete-document
  [doc]
  (db/delete-doc @document-db* (clj->js doc)))

(defn save-document
  [doc]
  (db/update-doc @document-db* (clj->js doc)))

(defn docs-of-type
  [doc-type]
  (db/query {:select "*" :where (str "type=" doc-type)}))

(defn get-document
  [id]
  (defer-node
    (db/get-doc @document-db* id)
    doc->record))

(defn all-documents
  []
  (let-realised [docs (db/all-docs @document-db*)]
    (util/await (map #(db/get-doc @document-db* (.-id %)) (.-rows @docs)))))
