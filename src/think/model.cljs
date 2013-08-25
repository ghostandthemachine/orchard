(ns think.model
  (:refer-clojure :exclude [create-node])
  (:require-macros
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require
    [think.util.core    :as util]
    [think.util.time    :as time]
    [cljs.core.async    :refer (chan >! <! timeout)]
    [think.util.log     :refer (log log-obj)]
    [clojure.string     :as string]
    [think.couchdb      :as db]
    [think.object       :as object]
    [dommy.template     :as tpl]))


(def ^:private cache* (atom {}))


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
  (swap! cache* dissoc (:id doc))
  (db/delete-doc @model-db* doc))


(defn save-document
  [doc]
  (m-log "save-document: ")
  (m-log-obj doc)
  (go
    (when-let [res (<! (db/update-doc @model-db*
                        (if (and (contains? doc :rev) (nil? (:rev doc)))
                          (dissoc doc :rev)
                          doc)))]
      (swap! cache* assoc (:id doc) res)
      res)))


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
    (if-let [cached-doc (get @cache* id)]
      (do
        (log "cached...")
        ; (log-obj cached-doc)
        cached-doc)
      (let [doc (<! (db/get-doc @model-db* id))]
        (swap! cache* assoc id doc)
        (log "not cached")
        ; (log-obj doc)
        doc))))


(defn load-document
  [id]
  (log "load-document: " id)
  (go
    (let [doc (<! (get-document id))]
      (log "load-document get-document return")
      (log-obj doc)
      (when doc
        (let [obj-type (keyword (:type doc))]
          (log "Doc defined in load doc: " (object/defined? obj-type))
          (if (object/defined? obj-type)
            (object/create obj-type doc)
            :no-matching-document-type))))))


; TODO: test me!  Not sure if mapping like this will work correctly.
(defn all-documents
  []
  (log "all-documents")
  (let [c (chan 10)]
    (go
      (let [docs (<! (db/all-docs @model-db*))]
        (if (= (:total_rows docs) 0)
          []
          (doseq [doc-row (:rows docs)]
            (if-let [doc (<! (db/get-doc @model-db* (:id doc-row)))]
              (>! c doc)
              (log (str "Could not load document: " doc-row)))))))))


(defn load-cache
  "Load the cache with all documents in the DB."
  []
  (go
    (reset! cache* (reduce (fn [docs doc]
                             (assoc docs (:id doc) doc))
                           {}
                           (<! (all-documents))))))


(defn all-wiki-documents
  []
  (log "all-wiki-documents")
  (go
    (let [docs (<! (db/view @model-db* :index :wiki-documents))]
      (if (:error docs)
        (log "Error: " (:error docs))
        (if (= (:total_rows (:value docs)) 0)
          []
          (map #(assoc % :id (:_id %)) (map :value (:rows (:value docs)))))))))


(defn delete-all-documents
  "Delete all documents."
  []
  (go
    (doseq [doc (<! (db/all-docs @model-db*))]
      (delete-document doc))))


(defn format-request
  [account]
  (str "http://" (:login account) ":" (:password account)
       "@" (:url account) (:root account)))


(defn synch-documents
  []
  (go
    (log "synch-docs")
    (let [target (format-request account)
          src    (:root account)
          put-c  (db/replicate-db target src)
          get-c  (db/replicate-db src target)
          a      (<! put-c)
          b      (<! get-c)]
      [a b])))


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
