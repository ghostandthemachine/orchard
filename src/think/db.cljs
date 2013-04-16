(ns think.db
  (:use-macros [redlobster.macros :only [when-realised defer-node]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]
            [think.util :refer [js->clj]]
            [think.log :refer (log log-obj log-err)]))

(def ^:private pouch (js/require "pouchdb"))


(defn uglify
  [x]
  (let [id  (:id x)
        rev (:rev x)
        x   (dissoc x :id :rev)]
      (if (and id rev)
        (assoc x "_id" id "_rev" rev)
        (assoc x "_id" id))))


(defn prettify
  [x]
  (let [id  (:_id x)
        rev (:_rev x)
        x   (dissoc x :_id :_rev)]
    (assoc x :id id :rev rev)))


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
  (defer-node (.post db (clj->js (uglify doc))) js->clj))


(defn put-doc
  "Insert a document with a given key."
  [db doc]
  (defer-node (.put db (clj->js (uglify doc))) js->clj))


(defn delete-doc
  "Delete a document."
  [db doc]
  (defer-node (.remove db (clj->js (uglify doc))) js->clj))


(defn all-docs
  "Get all documents in the DB."
  [db & opts]
  (defer-node (.allDocs db (clj->js (merge {} opts))) js->clj))


(defn -doc->clj
  "Recursively transforms JavaScript arrays into ClojureScript
  vectors, and JavaScript objects into ClojureScript maps.  With
  option ':keywordize-keys true' will convert object fields from
  strings to keywords."
  [x & options]
  (let [{:keys [keywordize-keys force-obj]} options
        keyfn (if keywordize-keys keyword str)
        f (fn thisfn [x]
            (cond
              (seq? x)         (doall (map thisfn x))
              (coll? x)        (into (empty x) (map thisfn x))
              (goog.isArray x) (vec (map thisfn x))
              (or force-obj
                  (identical? (type x) js/Object)
                  (identical? (type x) js/global.Object))
              (into {} (for [k (js-keys x)]
                         (if (= k "type")
                           [(keyfn k) (keyword (aget x k))]
                           [(keyfn k) (thisfn (aget x k))])))
              :else x))]
    (f x)))


(defn get-doc
  "Get a single document by ID."
  [db id & opts]
  (let [id-str (if (keyword? id)
                 (name id)
                 (str id))]
    (defer-node (.get db id-str (clj->js (merge {} opts)))
      (fn [doc]
        (log "get-doc: ")
        (log-obj doc)
        (prettify (-doc->clj doc :keywordize-keys :forc-obj))))))


(defn update-doc
  "Insert or modify a document, which must have a key \"_id\" or :_id."
  [db doc]
  (defer-node (.put db (clj->js (uglify doc))) js->clj))


(defn view
  "Generate a DB view using a mapping function, and optionally a reduce function."
  [db map-fn & [reduce-fn]]
  (if reduce-fn
    (defer-node (.query db {:map map-fn} {:reduce reduce-fn}) js->clj)
    (defer-node (.query db {:map map-fn}) js->clj)))

(defn query
  "Run a GQL query against the database."
  [db q]
  (defer-node (.gql db (clj->js q))))

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
