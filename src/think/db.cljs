(ns think.db
  (:use [think.log :only (log log-obj)])
  (:require-macros [think.macros :as mac]))

(def ^:private pouch   (js/require "pouchdb"))
(def db* (atom nil))

(defn handle-error
  [err]
  (log-obj err))


(defn db-open
  [path]
  (pouch (str "leveldb://" path) (cljs.core/clj->js {})
       (fn [err res]
         (if err
           (log "ERROR: ", err)
           (do
             (reset! db* res)
             (log "DB: ", res))))))


(defn db-get
  ([db id] (db-get db id nil))
  ([db id f]
   (.get db (clj->js id)
         (fn [err resp]
           (if err
             (handle-error err)
             (when f
               (f db (js->clj resp))))))
   db))


(defn db-put
  ([db doc] (db-put db doc nil))
  ([db doc f]
   "Insert or modify a document, which must have a key \"_id\" or :_id."
   (.put db (clj->js doc)
         (fn [err resp]
           (if err
             (handle-error err)
             (when f
               (f db (js->clj resp))))))
   db))


(defn db-remove
  ([db doc] (db-remove db doc nil))
  ([db doc f]
   (.remove db (clj->js doc)
            (fn [err resp]
              (if err
                (handle-error err)
                (when f
                  (f db (js->clj resp))))))
   db))


(defn db-info
  [db f]
  (.info db
         (fn [err resp]
           (if err
             (handle-error err)
             (f db (js->clj resp))))))


(defn db-replicate
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  ([src tgt] (db-replicate src tgt nil))
  ([src tgt f]
   (.replicate js/Pouch src tgt (clj->js {})
               (fn [err resp]
                 (if err
                   (handle-error err)
                   (when f
                     (f (js->clj resp))))))))



(defn db-insert
  [db doc]
  (mac/with-instance db
    (db-put doc)))


(defn db-update
  [db id f]
  (mac/with-instance db
    (db-get id f)))


(defn db-delete
  [db id]
  (db-update db id db-remove))


