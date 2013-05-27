(ns think.pouchdb
  (:use-macros [redlobster.macros :only [when-realised defer-node let-realised]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]
            [think.util :as util]
            [think.util.log :refer (log log-obj log-err)]))

(comment

(js/require "pouchdb")
(def ^:private pouch (js/require "pouchdb"))


(defn pouch-ids
  [x]
  (let [id  (:id x)
        rev (:rev x)
        x   (dissoc x :id :rev)]
      (if (and id rev)
        (assoc x "_id" id "_rev" rev)
        (assoc x "_id" id))))


(defn cljs-ids
  [x]
  (let [id   (:_id x)
        rev  (:_rev x)
        type (:type x)
        x    (dissoc x :_id :_rev :type)]
    (assoc x :id id :rev rev :type type)))


(defn promise-callback
  [p]
  (fn [err res]
    (if err
      (p/realise-error p (util/js->clj err))
      (p/realise p res))))


;; Document Database API

(def DB-STORAGE "leveldb://") ; currently the only one supported

(defn open
  [path]
  (let [pouch-promise (p/promise)]
    (pouch (str DB-STORAGE path) (cljs.core/clj->js {}) (promise-callback pouch-promise))
    pouch-promise))


(defn info
  "Returns some info about the state of the DB."
  [db]
  (let [info-promise (p/promise)]
    (.info db (promise-callback info-promise))
    info-promise))


(defn create-doc
  "Insert or modify a document. Respoinse will contain the generated key."
  [db doc]
  (defer-node (.post db (clj->js (pouch-ids doc))) util/js->clj))


(defn put-doc
  "Insert a document with a given key."
  [db doc]
  ; (log "db/update-doc")
  (log-obj (pouch-ids doc))
  (defer-node (.put db (clj->js (pouch-ids doc))) util/js->clj))


(defn delete-doc
  "Delete a document."
  [db doc]
  (defer-node (.remove db (clj->js (pouch-ids doc))) util/js->clj))


(defn all-docs
  "Get all documents in the DB."
  [db & opts]
  (defer-node (.allDocs db (clj->js (merge {} opts)))
    #(util/js->clj % :keywordize-keys true)))


(defn get-doc
  "Get a single document by ID."
  [db id & opts]
  (let [id-str (if (keyword? id)
                 (name id)
                 (str id))]
    (defer-node (.get db id-str (clj->js (merge {} opts)))
      (fn [doc]
        (cljs-ids (util/js->clj doc :keywordize-keys true :forc-obj true))))))


(defn update-doc
  "Insert or modify a document, which must have a key \"_id\" or :_id."
  [db doc]
  ; (log "db/update-doc")
  (log-obj (clj->js doc))
  (defer-node (.put db (clj->js (pouch-ids doc))) util/js->clj))


(defn map-reduce
  "Generate a DB view using a mapping function, and optionally a reduce function."
  [db map-fn & [reduce-fn]]
  (let [mapper (fn [doc emit] (map-fn (js->clj doc) emit))]
    (if reduce-fn
      (defer-node (.query db (clj->js {:map mapper}) (clj->js {:reduce reduce-fn})) util/js->clj)
      (defer-node (.query db (clj->js {:map mapper})) util/js->clj))))

(defn query
  "Run a GQL query against the database."
  [db q]
  (defer-node (.gql db (clj->js q))))

(defn replicate-docs
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  [src tgt]
  (defer-node (.replicate js/Pouch src tgt (clj->js {})) util/js->clj))

)
