(ns orchard.model
  (:require-macros
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require
    [orchard.util.core    :as util]
    [orchard.util.time    :as time]
    [cljs.core.async      :refer (chan >! <! timeout)]
    [orchard.util.log     :refer (log log-obj)]
    [orchard.couchdb      :as db]
    [orchard.kv-store     :as kv]
    [orchard.object       :as object]))


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
            (log "db connected")
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
  [id doc]
  (m-log "save-document: ")
  (m-log-obj doc)
  (go
    (when-let [res (<! (db/update-doc @model-db*
                        (if (and (contains? doc :rev) (nil? (:rev doc)))
                          (dissoc doc :rev)
                          doc)))]
      (swap! cache* assoc id res)
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
  (log "get-document: " id)
  (go
    (if-let [cached-doc (get @cache* id)]
      (do
        (log "cached...")
        ; (log-obj cached-doc)
        cached-doc)
      (let [_ (log "not cached")
            doc (<! (db/get-doc @model-db* id))]
        (swap! cache* assoc id doc)
        ; (log-obj doc)
        doc))))


(defn search-by-title
  [title]
  (log "search for document")
  (db/search @model-db* :index :docs-by-title {:q title}))



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


(defn all-projects
  []
  (log "all-projects")
  (go
    (let [docs (<! (db/view @model-db* :index :docs-by-project))]
      (if (:error docs)
        (log "Error: " (:error docs))
        (if (= (:total_rows (:value docs)) 0)
          []
          (map #(assoc % :id (or (:_id %) (:id %))) (map :value (:rows (:value docs)))))))))


(defn all-documents-by-title
  []
  (log "all-documents-by-title")
  (go
    (let [docs (<! (db/view @model-db* :index :docs-by-title))]
      (if (:error docs)
        (log "Error: " (:error docs))
        (if (= (:total_rows (:value docs)) 0)
          []
          (map #(assoc % :id (or (:_id %) (:id %))) (map :value (:rows (:value docs)))))))))


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


(defprotocol ObjectStore
  (save-object! [this id value]
    "Saves {:id value}.")

  (get-object   [this id]
    "Returns (:id store)."))

(defprotocol ObjectIndex
  (index-object [this id value]
    "Add this object to the index.")

  (lookup [this index obj-key]
    "Returns a channel of modules which ")

  (module-by [this index k]
    "Returns a channel of modules for which (pred module) => true"))


(defn load-object
  [db id]
  (log "load-object " id)
  (go
    (let [doc (<! (get-object db id))]
      (log-obj doc)
      (when doc
        (let [obj-type (keyword (:type doc))]
          (log "loading object type: " obj-type " defined? " (object/defined? obj-type))
          (if (object/defined? obj-type)
            (object/create obj-type doc)
            :no-matching-document-type))))))


(deftype CouchStore
  []
  ObjectStore
    (save-object! [this id value]
      (save-document id value))

    (get-object [this id]
      (log "couch-store get-object")
      (get-document id)))


(defn couch-store
  []
  (load-db)
  (load-cache)
  (CouchStore.))


(defn app-id
  [id]
  (let [id (if (keyword id) (name id) (str id))]
    (str "orchard/" id)))


(deftype LocalStore
  []
  ObjectStore
  (save-object! [this id value]
      (kv/local-set (app-id id) value))

  (get-object [this id]
    (go (kv/local-get (app-id id)))))

  ;ObjectIndex
  ;  (modules-by-type [this m-type]
  ;    (js/Object.keys (kv/local-get (str "orchard/type-index/" m-type)))))
;(when-let [t (:type value)]
;      (kv/local-set (str "orchard/type-index/" t)
;                    (conj (kv/local-get (str "orchard/type-index/" t)) value)))

(defn local-store
  []
  (LocalStore.))


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

