(ns think.couchdb
  (:require-macros
    [think.macros :refer [defonce node-chan]]
    [redlobster.macros :refer [when-realised let-realised defer-node]]
    [cljs.core.async.macros :as m :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! put! timeout close!]]
    [redlobster.promise :as p]
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
              (:_id x)
              (:id x))
        rev  (or
              (:_rev x)
              (:rev x))
        type (:type x)
        x    (dissoc x :_id :_rev)]
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
   (go
    (:value
      (<! (node-chan (.list (.-db server)) array-seq))))))


(defn replicate-db
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  ([src tgt]
   (replicate-db nano src tgt))
  ([server src tgt]
   (go
    (:value 
      (<! (node-chan (.replicate (.-db server) src tgt (clj->js {}))))))))


(defn create-db
  "Create a new database on the server."
  [server db-name]
  (go
    (:value 
      (<! (node-chan (.create (.-db server) db-name))))))


(defn open
  "Open a database by name."
  ([db-name] (open nano db-name))
  ([server db-name]
   (go
     (let [dbs (<! (list-all server))]
       (if (some #{db-name} dbs))
       (log (str "Using existing database: " db-name))
       (do
         (log (str "Creating new database: " db-name))
         (<! (create-db server db-name))))
       (.use server db-name))))


(defn delete-db
  "Delete a database."
  [db-name]
  (go
    (:value (<! (node-chan (.destroy (.-db nano) db-name) util/js->clj)))))


(defn info
  "Returns some info about the state of the DB."
  ([db-name] (info nano db-name))
  ([server db-name]
   (go
    (:value 
      (<! (node-chan (.get (.-db server) db-name)))))))


(defn delete-doc
  "Delete a document."
  ([db doc] (delete-doc db (:id doc) (:rev doc)))
  ([db doc-id doc-rev]
   (go
    (:value 
      (<! (node-chan (.destroy db doc-id doc-rev) util/js->clj))))))


(defn all-docs
  "Get all documents in the DB."
  [db & opts]
  (go
    (:value 
      (<! (node-chan (.list db (util/clj->js (merge {} opts)))
    #(util/js->clj % :keywordize-keys true))))))


(defn get-doc
  "Get a single document by ID."
  [db id & opts]
  (let [id-str (str id)]
    (go
      (:value
        (<! (node-chan (.get db id-str (clj->js (merge {} opts)))
              (fn [doc]
                (cljs-ids (util/js->clj doc :keywordize-keys true :forc-obj true)))))))))


(defn update-doc
  "Insert or modify a document. If the doc has an :id field it will be used as the document key."
  [db doc]
  (let [res (if (:id doc)
              (node-chan (.insert db (clj->js (couch-ids doc)) (str (:id doc))))
              (node-chan (.insert db (clj->js (couch-ids doc)))))]
    (go
      (let [v (js->clj (<! res))]
        (log "update-doc result:")
        (log-obj v)
        (if (:error v)
          (do
            (log "Error in update-doc:")
            (log-obj (:error v)))
          doc)))))


(defn view
  "Return a view result."
  [db design view-name]
  (node-chan (.view db (name design) (name view-name)) #(util/js->clj % :keywordize-keys true)))


(defn create-view
  [db design view-name map-str & [reduce-str]]
  (let [design (str "_design/" design)
        view (if reduce-str
               {:map map-str :reduce reduce-str}
               {:map map-str})
        doc {:views {view-name view}}]
    (go
      (update-doc db (clj->js doc)))))


"""
To create a view you need to create a design document, which can have a view.  In the futon utility for couch here http://localhost:5984/_utils/ you can create a temporary view and then save it permanently.  The view for the wiki-document index looks like this:
"""

(def index-view
  "function(doc) {
    if(doc.type == 'wiki-document' && doc._id != ':home') {
        emit(doc._id, doc);
      }
}")


(comment defn map-reduce
  "Generate a DB view using a mapping function, and optionally a reduce function."
  [db map-fn & [reduce-fn]]
  (let [mapper (fn [doc emit] (map-fn (js->clj doc) emit))]
    (if reduce-fn
      (defer-node (.query db (util/clj->js {:map mapper}) (util/clj->js {:reduce reduce-fn})) util/js->clj)
      (defer-node (.query db (clj->js {:map mapper})) util/js->clj))))


