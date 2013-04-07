(ns think.db
  (:use [think.log :only (log log-obj log-err)])
  (:use-macros [redlobster.macros :only [promise when-realised]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]))

(def ^:private pouch (js/require "pouchdb"))

(defn uglify-id
  [m]
  (let [id (:id m)]
    (-> m
      (dissoc :id)
      (assoc "_id" id))))

(defn pretify-id
  [m]
  (let [id ("_id" m)]
    (-> m
      (dissoc "_id")
      (assoc :id id))))

(defn handle-error
  [err]
  (log-obj err))

(defn handle-realised
  [p err resp]
  (if err
    (log "Error ")
    (p/realise p resp)))

(defn db-open
  [path]
  (let [pouch-promise (p/promise)]
    (pouch (str "leveldb://" path) (cljs.core/clj->js {})
      (fn [err resp]
        (when-not err
          (p/realise pouch-promise resp))))
    pouch-promise))



(comment
(do
  (def db* (atom nil))


  (def db-promise (db-open "dev.db"))

  (reset! db* @db-promise)

  (log "db id is: " (.id @db*))

  (def put-promise
    (db-put @db* {:id "woz" :title "boner"}))

  (p/on-realised put-promise
    #(log "err " %)
    #(log "success " %))



  (db-get @db* "woz" #(log "success " %) #(log "err " %))

  (log "all docs" (.allDocs @db*))

  (log "owng")

  (reset! db* nil)
)
)


(defn db-get
  [db id]
  (let [get-promise (p/promise)]
    (.get db (name id) (partial handle-realised get-promise))
    get-promise))

(defn db-put
  "Insert or modify a document, which must have a key \"_id\" or :_id."
  [db doc]
  (let [put-promise (p/promise)]
    (.put db (clj->js doc) (partial handle-realised put-promise))
    put-promise))

(defn db-post
  "Insert or modify a document. Respoinse will contain the generated key."
  [db doc]
  (let [post-promise (p/promise)]
    (.post db doc (partial handle-realised post-promise))
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
  [db]
  (let [res-promise (p/promise)]
    (.transaction db
      (fn [tx]
        (.executeSql tx query (clj->js [])
          (fn [tx results]
            (p/realise res-promise results)))))))


(defn local-store-set
  [k v]
  (aset js/localStorage k v)
  v)


(defn local-store-get
  [k]
  (aget js/localStorage k))


