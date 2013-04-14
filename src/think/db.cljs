(ns think.db
  (:use-macros [redlobster.macros :only [when-realised defer-node]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]
            [think.log :refer (log log-obj log-err)]))

(def ^:private pouch (js/require "pouchdb"))


(defn uglify-id
  [m]
  (if (contains? m :id)
    (let [id (:id m)
          less-id (dissoc m :id)]
      (assoc less-id "_id" id))
    m))


(defn prettify-id
  [m]
  (let [id (:_id m)
        less-val (dissoc :_id)]
    (assoc less-val :id id)))


(defn promise-callback
  [p]
  (fn [err res]
    (if err
      (p/realise-error p (js->clj err))
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
  (defer-node (.post db (clj->js (uglify-id doc))) js->clj))


(defn put-doc
  "Insert a document with a given key."
  [db doc]
  (defer-node (.put db (clj->js (uglify-id doc))) js->clj))


(defn delete-doc
  "Delete a document."
  [db doc]
  (defer-node (.remove db (clj->js (uglify-id doc))) js->clj))


(defn all-docs
  "Get all documents in the DB."
  [db & opts]
  (defer-node (.allDocs db (clj->js (merge {} opts))) js->clj))


(defn get-doc
  "Get a single document by ID."
  [db id & opts]
  (defer-node (.get db (name id) (clj->js (merge {} opts))) (comp prettify-id js->clj)))


(defn update-doc
  "Insert or modify a document, which must have a key \"_id\" or :_id."
  [db doc]
  (defer-node (.put db (clj->js (uglify-id doc))) js->clj))


(defn replicate-docs
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  [src tgt]
  (defer-node (.replicate js/Pouch src tgt (clj->js {})) js->clj))

;; SQL Database API

(def DEFAULT-SQL-DB-SIZE (* 1024 1024))

(defn sql-store
  "Returns a named, local WebSQL database."
  ([db-name] (sql-store db-name "1.0" db-name DEFAULT-SQL-DB-SIZE))
  ([db-name version description size]
   (js/openDatabase db-name version description size)))

(defn sql-exec
  "Execute a SQL statement on the db."
  [db stmt]
  (let [res-promise (p/promise)]
    (.transaction db
      (fn [tx]
        (.executeSql tx stmt (clj->js [])
          (fn [tx results]
            (p/realise res-promise results)))))
    res-promise))


;; Local Key-Value Store API

(defn local-set
  "Set a key/value pair in the persistent local storage."
  [k v]
  (aset js/localStorage k v)
  v)


(defn local-get
  "Get a value in the persistent local storage by key."
  [k]
  (aget js/localStorage k))
