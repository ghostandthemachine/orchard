(ns think.model
  (:refer-clojure :exclude [create-node])
  (:require-macros
    [redlobster.macros :refer [when-realised let-realised defer-node]]
    [cljs.core.async.macros :as m :refer [go alt! alts!]])
  (:require
    [think.util.core    :as util]
    [think.util.time    :as time]
    [cljs.core.async :refer (chan >!! <!! close! thread timeout)]
    [think.util.log :refer (log log-obj)]
    [clojure.string :as string]
    [think.couchdb      :as db]
    [think.object       :as object]
    [redlobster.promise :as p]
    [dommy.template     :as tpl]))


(def do-log true)

(defn m-log
  [& args]
  (when do-log
    (apply log args)))


(defn m-log-obj
  [& args]
  (when do-log
    (apply log-obj args)))


(def account
 {:login    "jon"
  :password "celerycatstick"
  :url      "lifeisagraph.com:5984/"
  :root     "projects"})


(def DEFAULT-DB "projects")
(def CONNECT-RETRY-MS 100)

(def model-db* (atom nil))

(defn try-connect
  [db-name]
  (log "model try-connect...")
  (go
    (loop [db-chan (db/open db-name)]
      (let [db (<! db-chan)]
        (if (nil? db)
          (do
            (<! (timeout CONNECT-RETRY-MS))
            (recur (db/open db-name)))
          (do
            (reset! model-db* db)
             db))))))


(defn load-db
  ([] (load-db DEFAULT-DB))
  ([db-name]
   (log "starting database...")
   (db/start!)
   (log "loading database: " db-name)
   (try-connect db-name)))


(defn delete-document
  [doc]
  (m-log "delete-document")
  (db/delete-doc @model-db* doc))


(defn save-document
  [doc]
  (m-log "save-document: ")
  (m-log-obj doc)
  (db/update-doc @model-db*
                 (if (and (contains? doc :rev) (nil? (:rev doc)))
                   (dissoc doc :rev)
                   doc)))


(comment defn docs-of-type
  [doc-type]
  (let [doc-type (if (keyword? doc-type) (name doc-type) (str doc-type))
        mapper (fn [doc]
                 (when (= doc-type (:type (js->clj doc)))
                   (emit (.-:id doc) doc)))]
    (db/map-reduce @model-db* mapper)))

    ;(db/query @model-db* {:select "*" :where (str "type=" doc-type)})))


(defn get-document
  [id]
  (m-log "get-document: " id)
  (go
    (let [res (<! (db/get-doc @model-db* id))]
      (if (:error res)
        nil
        (:value res)))))


(defn load-document
  [id]
  (log "load-document: " id)
  (go
    (let [doc (<! (get-document id))]
      (when doc
        (let [obj-type (keyword (:type doc))]
          (if (object/defined? obj-type)
            (object/create obj-type doc)
            :no-matching-document-type))))))


(defn all-documents
  []
  (let-realised [docs (db/all-docs @model-db*)]
    (if (= (:total_rows @docs) 0)
      []
      (util/await (map #(db/get-doc @model-db* (:id %)) (:rows @docs))))))

(defn all-wiki-documents
  []
  (let-realised [docs (db/view @model-db* :index :wiki-documents)]
    (if (= (:total_rows @docs) 0)
      []
      (map #(assoc % :id (:_id %)) (map :value (:rows @docs))))))
      ;(util/await (map #(db/get-doc @model-db* (:id %)) (:rows @docs))))))


(defn delete-all-documents
  "Delete all documents."
  []
  (let-realised [docs (all-documents)]
    (util/await (doall (map #(db/delete-doc @model-db* %) @docs)))))


(defn format-request
  [account]
  (str
    "http://"
    (:login account)
    ":"
    (:password account)
    "@"
    (:url account)
    (:root account)))


(defn synch-documents
  []
  (log "synch-docs")
  (let [target  (format-request account)
        src     (:root account)
        putp    (db/replicate-db target src)
        getp    (db/replicate-db src target)]
    (util/await [putp getp])))



(defn media-document
  [{:keys [title authors path filename notes annotations cites tags] :as doc}]
  (assoc doc
         :type :media-document
         :id         (util/uuid)
         :type       type
         :created-at (util/date-json)
         :updated-at (util/date-json)))


(defn wiki-document
  [{:keys [rev title template] :as doc}]
  (assoc doc
         :type :wiki-document
         :id         (util/uuid)
         :created-at (util/date-json)
         :updated-at (util/date-json)))

