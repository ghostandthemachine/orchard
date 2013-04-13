(ns think.db
  (:use-macros [redlobster.macros :only [promise when-realised]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]
            [think.log :refer (log log-obj log-err)]))

(def ^:private pouch (js/require "pouchdb"))


(defn uglify-id
  [m]
  (if (or
        (contains? m :id)
        (contains? m "id"))
    (let [id-key (if (contains? m :id)
                  :id
                  "id")
          id     (id-key m)
          less-id (dissoc m id-key)]
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
      (p/realise p (js->clj res)))))


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
  (let [post-promise (p/promise)]
    (.post db (clj->js doc) (promise-callback post-promise))
    post-promise))


(defn put-doc
  "Insert a document with a given key."
  [db k doc]
  (let [put-promise (p/promise)]
    (.put db (clj->js (assoc doc :_id k)) (promise-callback put-promise))
    put-promise))


(defn delete-doc
  "Delete a document."
  [db doc]
  (let [remove-promise (p/promise)]
    (.remove db (clj->js doc) (promise-callback remove-promise))
    remove-promise))


(defn all-docs
  "Get all documents in the DB."
  [db & opts]
  (let [all-docs-promise (p/promise)]
    (.allDocs db (clj->js (merge {} opts)) (promise-callback all-docs-promise))
    all-docs-promise))


(defn get-doc
  "Get a single document by ID."
  [db id & opts]
  (let [get-promise (p/promise)]
    (.get db (name id) (clj->js (merge {} opts)) (promise-callback get-promise))
    get-promise))


(defn update-doc
  "Insert or modify a document, which must have a key \"_id\" or :_id."
  [db doc]
  (let [put-promise (p/promise)
        udoc (uglify-id doc)]
    (.put db (clj->js udoc) (promise-callback put-promise))
    put-promise))


(defn replicate-docs
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  [src tgt]
  (let [replicate-promise (p/promise)]
    (.replicate js/Pouch src tgt (clj->js {}) (promise-callback replicate-promise))
    replicate-promise))

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
