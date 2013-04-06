(ns think.db)
(comment
;  (:use     [think.log  :only (log log-obj)])
;  (:require [think.util :as util]))


(def ^:private pouch   (js/require "pouchdb"))


(defn handle-error
  [err]
  (log-obj err))


(def db-open
  [path]
  (pouch (str "leveldb://" path) (cljs.core/clj->js {})
       (fn [err res]
         (if err
           (log "ERROR: ", err)
           (do
             (reset! db* res)
             (log "DB: ", res)))))


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



(defmacro with-instance
  [db-name & body]
  `(pouch ~db-name (cljs.core/clj->js {})
          (fn [err# db#]
            (-> db# ~@body))))


(defn insert
  [db doc]
  (with-instance db
    (db-put doc)))


(defn db-update
  [db id f]
  (with-db db
    (db-get id f)))


(defn db-delete
  [db id]
  (perform-on-doc db id db-remove))


