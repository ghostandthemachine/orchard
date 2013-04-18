(ns think.model
  (:refer-clojure  :exclude [create-node])
  (:use [think.util.log :only    [log log-obj]])
  (:use-macros [redlobster.macros :only [let-realised defer-node]]
               [think.macros :only [defui]])
  (:require [think.util         :as util]
            [think.dispatch     :refer [fire react-to]]
            [think.db           :as db]
            [redlobster.promise :as p]
            [dommy.template :as tpl]))


(def DB-PATH "data/document.db")

(def document-db* (atom nil))

(defmulti doc->record     #(keyword (:type %)))
(defmulti create-document #(keyword (:type %)))


(defmethod doc->record :default
  [doc]
  (log "doc->record - Missing or unsupported doc type: " doc)
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
  (db/delete-doc @document-db* doc))


(defn save-document
  [doc]
  (db/update-doc @document-db*
                 (if (and (contains? doc :rev) (nil? (:rev doc)))
                   (dissoc doc :rev)
                   doc)))


(defn docs-of-type
  [doc-type]
  (db/query @document-db* {:select "*" :where (str "type=" doc-type)}))


(defn get-document
  [id]
  (let [res-promise (p/promise)
        doc-promise (db/get-doc @document-db* id)]
    (p/on-realised doc-promise
      (fn success [doc]
        (p/realise res-promise (doc->record doc)))

      (fn error [err]
        (if (and (= (.-status err) 404)
                 (= (.-error err) "not_found"))
          (p/realise res-promise nil)
          (p/realise-error res-promise err))))
    res-promise))


(defn all-documents
  []
  (let-realised [docs (db/all-docs @document-db*)]
    (util/await (map #(db/get-doc @document-db* (.-id %)) (.-rows @docs)))))


(defrecord PDFDocument
  [type id rev created-at updated-at
   title authors path filename notes annotations cites tags])

(defrecord WikiDocument
  [type id rev created-at updated-at title template])

(defmethod doc->record :wiki-document
  [{:keys [type id rev template title created-at updated-at]}]
  (let [tpl (doc->record template)]
    (map->WikiDocument. {:type type :id id :rev rev :created-at created-at :updated-at updated-at
                         :title title
                         :template tpl})))


(defmethod create-document :wiki-document
  [{:keys [type id template title modules]}]
  (map->WikiDocument {:id         (or id (util/uuid))
                      :type       type
                      :created-at (util/date-json)
                      :updated-at (util/date-json)
                      :title      title
                      :template   template}))

(defrecord SingleColumnTemplate [type modules])

(defmethod doc->record :single-column-template
  [{:keys [modules]}]
  (map->SingleColumnTemplate.
    {:type :single-column-template
     :modules (map doc->record modules)}))

(defrecord MarkdownModule [text])


(defrecord HTMLModule [text])


(defmethod doc->record :markdown-module
  [module]
  (map->MarkdownModule module))

(defmethod doc->record :html-module
  [module]
  (map->HTMLModule module))


(defrecord FormModule     [fields])
(defrecord QueryModule    [query template])


(defn home-doc
  []
  (create-document
    {:type :wiki-document
     :id :home
     :template {:type :single-column-template
                :modules [{:type ::markdown-module
                           :text "## Now we can use markdown"
                           :id   (util/uuid)}
                          {:type ::html-module
                           :text "<h1> Or raw HTML </h1>"
                           :id   (util/uuid)}]}
     :title "thinker app"}))

(defn reset-home
  []
  (let-realised [doc (get-document :home)]
    (delete-document @doc)
    (save-document (home-doc))))


