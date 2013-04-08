(ns think.db
  (:use [think.log :only (log log-obj log-err)])
  (:use-macros [redlobster.macros :only [promise when-realised]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]))

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

(defn pretify-id
  [m]
  (let [id (or ("_id" m) (:_id m))]
    (-> m
      (dissoc "_id")
      (assoc :id id))))

(defn handle-error
  [err]
  (log-obj err))

(defn handle-realised
  [p err data]
  (if err
    (log "Error " err)
    (p/realise p (pretify-id data))))

(defn db-open
  [path]
  (let [pouch-promise (p/promise)]
    (pouch (str "leveldb://" path) (cljs.core/clj->js {})
      (fn [err data]
        (when-not err
          (p/realise pouch-promise data))))
    pouch-promise))

(defn db-get
  [db id & opts]
  (let [get-promise (p/promise)]
    (.get db (name id) (clj->js (merge {} opts)) (partial handle-realised get-promise))
    get-promise))

(defn db-all-docs
  [db & opts]
  (let [all-docs-promise (p/promise)]
    (.allDocs db (clj->js (merge {} opts)) (partial handle-realised all-docs-promise))
    all-docs-promise))

(defn db-put
  "Insert or modify a document, which must have a key \"_id\" or :_id."
  [db doc]
  (let [put-promise (p/promise)
        udoc (uglify-id doc)]
    (.put db (clj->js udoc) (partial handle-realised put-promise))
    put-promise))

(defn db-post
  "Insert or modify a document. Respoinse will contain the generated key."
  [db doc]
  (let [post-promise (p/promise)]
    (.post db (clj->js doc) (partial handle-realised post-promise))
    post-promise))

(defn db-remove
  [db doc]
  (let [remove-promise (p/promise)]
    (.remove db (clj->js doc) (partial handle-realised remove-promise))
    remove-promise))

(defn db-info
  [db success-handler]
  (let [info-promise (p/promise)]
    (.info db (partial handle-realised info-promise))
    info-promise))

(defn db-replicate
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  [src tgt]
  (let [replicate-promise (p/promise)]
    (.replicate js/Pouch src tgt (clj->js {}) (partial handle-realised replicate-promise))
    replicate-promise))


(def DEFAULT-SQL-DB-SIZE (* 1024 1024))

(defn sql-db
  ([db-name] (sql-db db-name "1.0" db-name DEFAULT-SQL-DB-SIZE))
  ([db-name version description size]
   (js/openDatabase db-name version description size)))


(defn sql-query
  [db query]
  (let [res-promise (p/promise)]
    (.transaction db
      (fn [tx]
        (.executeSql tx query (clj->js [])
          (fn [tx results]
            (p/realise res-promise results)))))
    res-promise))


(defn local-store-set
  [k v]
  (aset js/localStorage k v)
  v)


(defn local-store-get
  [k]
  (aget js/localStorage k))


(comment
  (def db* (atom nil))
  (def db-promise (db-open "foo.db"))

  (reset! db* @db-promise)
  (log "db id is: " (.id @db*))

  (def put-promise
    (db-put @db* {:id "11" :title "gwgwgr"}))

  (log @put-promise)

  (def get-promise
    (db-get @db* "we"))

  (log @get-promise)

  (def all-docs-promise
    (db-all-docs @db*))

  (log @all-docs-promise)

  clojure.browser.repl.connect("http://localhost:9000/repl")
)
