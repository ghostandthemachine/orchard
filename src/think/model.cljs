(ns think.model
  (:refer-clojure  :exclude [create-node])
  (:use [think.util.log :only    [log log-obj]])
  (:use-macros [redlobster.macros :only [when-realised let-realised defer-node]]
               [think.macros :only [defui]])
  (:require [think.util         :as util]
            [think.dispatch     :refer [fire react-to]]
            [think.db           :as db]
            [think.object       :as object]
            [redlobster.promise :as p]
            [dommy.template :as tpl]))




(def DB-PATH "data/document.db")


(object/behavior* ::init-db
                  :triggers #{:init-db}
                  :reaction (fn [this]
                              (log "Initialize db.. ")
                              (let-realised [db-promise (db/open DB-PATH)]
                                (object/merge! this
                                  {:ready? true
                                   :document-db* @db-promise})
                                (object/raise this :db-loaded))))

(object/behavior* ::db-loaded
                  :triggers #{:db-loaded}
                  :reaction (fn [this]
                              (log "PouchDB loaded...")
                              (fire :db-loaded)))



(object/object* ::model
                :triggers  #{:db-loaded  :init-db}
                :behaviors [::db-loaded ::init-db]
                :ready? false
                :init (fn [this]
                        (object/raise this :init-db)))


(def model (object/create ::model))


(defn delete-document
  [doc]
  (db/delete-doc (:document-db* @model) doc))


(defn save-document
  [doc]
  (db/update-doc (:document-db* @model)
                 (if (and (contains? doc :rev) (nil? (:rev doc)))
                   (dissoc doc :rev)
                   doc)))


(defn docs-of-type
  [doc-type]
  (let [doc-type (if (keyword? doc-type) (name doc-type) (str doc-type))]
    (db/query (:document-db* @model) {:select "*" :where (str "type=" doc-type)})))


(defn get-document
  [id]
  (let [res-promise (p/promise)
        doc-promise (db/get-doc (:document-db* @model) id)]
    (p/on-realised doc-promise
      (fn success [doc]
        (p/realise res-promise doc))

      (fn error [err]
        (if (and (= (.-status err) 404)
                 (= (.-error err) "not_found"))
          (p/realise res-promise nil)
          (p/realise-error res-promise err))))
    res-promise))


(defn all-documents
  []
  (let-realised [docs (db/all-docs (:document-db* @model))]
    (util/await (map #(db/get-doc (:document-db* @model) (.-id %)) (.-rows @docs)))))


(defn pdf-document
  [{:keys [title authors path filename notes annotations cites tags] :as doc}]
  (assoc doc
         :type :pdf-document
         :id         (util/uuid)
         :type       type
         :created-at (util/date-json)
         :updated-at (util/date-json)))


(defn wiki-document
  [{:keys [rev title template] :as doc}]
  (assoc doc
         :type :wiki-document
         :id         (util/uuid)
         :created-at (util/date-json)
         :updated-at (util/date-json)))


(defrecord FormModule     [fields])
(defrecord QueryModule    [query template])


(defn home-doc
  []
  {:type :wiki-document
   :id :home
   :template {:type :single-column-template
              :modules [{:type ::markdown-module
                         :text "## Now we can use markdown"
                         :id   (util/uuid)}
                        {:type ::html-module
                         :text "<h1> Or raw HTML </h1>"
                         :id   (util/uuid)}]}
   :title "thinker app"})


(defn reset-home
  []
  (let-realised [doc (get-document :home)]
    (delete-document @doc)
    (save-document (home-doc))))


