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

(defmulti doc->record     :type)
(defmulti create-document :type)

(defmethod doc->record :default
  [doc]
  (log "doc->record - Missing or unsupported doc type:")
  (log-obj doc))

(defn init-document-db
  []
  (when (nil? @document-db*)
    (let [db-promise (db/open DB-PATH)]
      (p/on-realised db-promise
        #(do
           (reset! document-db* %)
           (fire :document-db-ready @document-db*))
        (fn [err]
          (log "Error opening DB!")
          (log-obj err))))))


(defn delete-document
  [doc]
  (db/delete-doc @document-db* (clj->js doc)))

(defn save-document
  [doc]
  (db/update-doc @document-db* doc))

(defn docs-of-type
  [doc-type]
  (db/query @document-db* {:select "*" :where (str "type=" doc-type)}))

(defn get-document
  [id]
  (defer-node
    (.get @document-db* (if (keyword? id)
                  (name id)
                  (str id))
          (clj->js {}))
    (fn [doc]
      (log-obj doc)
      (doc->record doc))))

;          (fn [err res] (log-obj res))))
;    (db/get-doc @document-db*
;                )
;    doc->record))

(defn all-documents
  []
  (let-realised [docs (db/all-docs @document-db*)]
    (util/await (map #(db/get-doc @document-db* (.-id %)) (.-rows @docs)))))

(defrecord PDFDocument
  [type id rev created-at updated-at
   title authors path filename notes annotations cites tags])

(defrecord WikiDocument
  [type id rev created-at updated-at
   template title modules])

(defmethod doc->record :wiki-document
  [{:keys [type id rev template title modules created-at updated-at]}]
  (let [mods (map doc->record modules)
        tpl  (doc->record {:type (keyword template) :modules mods})]
    (WikiDocument. type id rev tpl title mods created-at updated-at)))

(defmethod create-document :wiki-document
  [{:keys [type id template title]}]
  (map->WikiDocument {:id         (or id (util/uuid))
                      :type       type
                      :template   template
                      :title      title
                      :modules    []
                      :created-at (util/date-json)
                      :updated-at (util/date-json)}))

(defrecord SingleColumnTemplate [modules])

(defmethod doc->record :single-column-template
  [{:keys [modules]}]
  (SingleColumnTemplate. modules))


(defrecord MarkdownModule [text])

(defmethod doc->record :markdown-module
  [module]
  (map->MarkdownModule module))


(defrecord FormModule     [fields])
(defrecord QueryModule    [query template])


