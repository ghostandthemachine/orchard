(ns think.couchdb
  (:use-macros [redlobster.macros :only [when-realised defer-node let-realised]])
  (:require-macros [think.macros :refer [defonce]])
  (:require [redlobster.promise :as p]
            [think.object :as object]
            [think.util.core :as util]
            [think.util.os :as os]
            [think.util.log :refer (log log-obj log-err)]))


(def db* (atom nil))

(defn log-handler
  [log-id msg]
  (object/raise think.objects.logger/logger :post log-id msg))


(defn running?
  []
  (if @db* true false))


(defonce db-proc (os/process "couchdb"))


(defn- start-db
  []
  (doseq [pipe [(.-stdout db-proc) (.-stderr db-proc)]]
    (.on pipe "data"
      (partial log-handler :couchdb)))
  db-proc)


(defn start!
  []
  (reset! db* (start-db)))


(def ^:private couch-server (js/require "nano"))
(def ^:private nano         (couch-server "http://localhost:5984"))

(defn couch-ids
  [x]
  (let [id  (:id x)
        rev (:rev x)
        x   (dissoc x :id :rev)]
      (if (and id rev)
        (assoc x "_id" id "_rev" rev)
        (assoc x "_id" id))))


(defn cljs-ids
  [x]
  (let [id   (or
              ("_id" x)
              (:id x))
        rev  (or
              ("_rev" x)
              (:rev x))
        type (:type x)
        x    (dissoc x "_id" "_rev" :type)]
    (assoc x :id id :rev rev :type type)))


(defn promise-callback
  [p]
  (fn [err res]
    (if err
      (p/realise-error p (util/js->clj err))
      (p/realise p res))))


;; Document Database API

(defn list-all
  "List all the DBs available on this server."
  ([] (list-all nano))
  ([server]
   (defer-node (.list (.-db server)))))


(defn replicate-db
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  ([src tgt]
   (replicate-db nano src tgt))
  ([server src tgt]
   (defer-node (.replicate (.-db server) src tgt (clj->js {})))))


(defn open
  "Open a database by name."
  ([db-name] (open nano db-name))
  ([server db-name]
   (let [db-promise (p/promise)]
     (let-realised [dbs (list-all server)]
       (if (some #{db-name} @dbs)
         (p/realise db-promise (.use server db-name))
         (let-realised [db-res (.create (.-db server) db-name)]
           (p/realise db-promise (.use server db-name))))
       db-promise))))


(defn delete-db
  "Delete a database."
  [db-name]
  (defer-node (.destroy (.-db nano) db-name) util/js->clj))


(defn info
  "Returns some info about the state of the DB."
  ([db-name] (info nano db-name))
  ([server db-name]
   (defer-node (.get (.-db server) db-name))))


(defn delete-doc
  "Delete a document."
  ([db doc] (delete-doc db (:id doc) (:rev doc)))
  ([db doc-id doc-rev]
   (defer-node (.destroy db doc-id doc-rev) util/js->clj)))


(defn all-docs
  "Get all documents in the DB."
  [db & opts]
  (defer-node (.list db (util/clj->js (merge {} opts)))
    #(util/js->clj % :keywordize-keys true)))


(defn get-doc
  "Get a single document by ID."
  [db id & opts]
  (let [id-str (str id)]
    (defer-node (.get db id-str (clj->js (merge {} opts)))
      (fn [doc]
        (cljs-ids (util/js->clj doc :keywordize-keys true :forc-obj true))))))


(defn update-doc
  "Insert or modify a document. If the doc has an :id field it will be used as the document key."
  [db doc]
  (log "update-doc:")
  (log-obj (clj->js doc))
  (let [doc-promise (p/promise)
        cb (fn [err res]
             (if err
               (p/realise-error doc-promise (util/js->clj err))
               (p/realise doc-promise (assoc doc :rev (.-rev res)))))]
    (if-let [doc-id (:id doc)]
      (.insert db (clj->js (couch-ids doc)) (str doc-id) cb)
      (.insert db (clj->js (couch-ids doc)) cb))
    ; (log "update promise")
    ; (log-obj doc-promise)
    ; (log-obj @doc-promise)
    doc-promise))


(defn view
  "Return a view result."
  [db design view-name]
  (defer-node (.view db (name design) (name view-name)) #(util/js->clj % :keywordize-keys true)))


(comment defn map-reduce
  "Generate a DB view using a mapping function, and optionally a reduce function."
  [db map-fn & [reduce-fn]]
  (let [mapper (fn [doc emit] (map-fn (js->clj doc) emit))]
    (if reduce-fn
      (defer-node (.query db (util/clj->js {:map mapper}) (util/clj->js {:reduce reduce-fn})) util/js->clj)
      (defer-node (.query db (clj->js {:map mapper})) util/js->clj))))



