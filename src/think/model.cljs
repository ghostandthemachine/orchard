(ns think.model
  (:refer-clojure :exclude [create-node])
  (:require-macros
    [redlobster.macros :refer [when-realised let-realised defer-node]]
    [cljs.core.async.macros :as m :refer [go alt! alts!]])
  (:require
    [think.util.core    :as util]
    [think.util.time    :as time]
    [cljs.core.async :refer (chan >!! <!! close! thread)]
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
  [res-prom db-name]
  (log "model try-connect...")
  (let [db-prom (db/open db-name)]
    (p/on-realised db-prom
                   (fn on-success []
                     (log "couchdb connected!")
                     (reset! model-db* @db-prom)
                     (p/realise res-prom @db-prom))
                   (fn on-failure []
                     (log "couchdb connect failed...")
                     (time/run-in CONNECT-RETRY-MS #(try-connect res-prom db-name))))))


(defn load-db
  ([] (load-db DEFAULT-DB))
  ([db-name]
   (log "starting database...")
   (db/start!)
   (let [res-prom (p/promise)]
     (log "loading database: " db-name)
     (try-connect res-prom db-name)
     res-prom)))


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
  (let [res-promise (p/promise)
        doc-promise (db/get-doc @model-db* id)]
    (p/on-realised doc-promise
      (fn success [doc]
        (p/realise res-promise doc))

      (fn error [err]
        (if (and (= (.-status err) 404)
                 (= (.-error err) "not_found"))
          (p/realise res-promise nil)
          (p/realise-error res-promise err))))
    res-promise))


(defn load-document
  [id]
  (log "load-document: " id)
  (let [res-promise (p/promise)
        doc-promise (get-document id)]
    (p/on-realised doc-promise
      (fn success [doc]
        (let [obj-type (keyword (:type doc))]
          (if (object/defined? obj-type)
            (p/realise res-promise (object/create obj-type doc))
            (p/realise res-promise :no-matching-type))))

      (fn error [err]
        (if (and (= (.-status err) 404)
                 (= (.-error err) "not_found"))
          (p/realise res-promise nil)
          (p/realise-error res-promise err))))
    res-promise))


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


(defrecord FormModule     [fields])
(defrecord QueryModule    [query template])


(defn markdown-module
  []
  {:type :markdown-module
   :text "## Markdown module"
   :id   (util/uuid)})


(defn html-module
  []
  {:type :html-module
   :text "<button class=\"btn\" onclick=\"think.objects.new$.load_new_doc();\">New</button>"
   :id   (util/uuid)})

(defn media-module
  []
  {:type :media-module
   :path "test.pdf"
   :id (util/uuid)})


(defn index-module
  []
  {:type :index-module
   :id (util/uuid)})


(defn single-column-template
  [& mod-ids]
  {:type :single-column-template
   :modules mod-ids
   :id (util/uuid)})


(defn home-doc
  [tpl-id]
  {:type :wiki-document
   :id :home
   :template tpl-id
   :title "thinker app"})


(defn test-doc
  [id tpl-id]
  {:type :wiki-document
   :id id
   :template tpl-id
   :title (str "test document " id)})


(defn create-home
  []
  (let [index      (index-module)
        ht-doc     (html-module)
        tpl-doc    (single-column-template (:id index) (:id ht-doc))
        home-doc   (home-doc (:id tpl-doc))]
    (doseq [doc [index ht-doc tpl-doc home-doc]]
      (save-document doc))
    home-doc))


(defn create-test-doc
  []
  (let [md-doc     (markdown-module)
        ht-doc     (html-module)
        tpl-doc    (single-column-template (:id md-doc) (:id ht-doc))
        test-doc   (test-doc :test-doc1 (:id tpl-doc))]
    (doseq [doc [md-doc ht-doc tpl-doc test-doc]]
      (save-document doc))))


(defn create-media-doc
  []
  (let [md-doc     (markdown-module)
        media-doc  (media-module)
        tpl-doc    (single-column-template (:id md-doc) (:id media-doc))
        test-doc   (test-doc :test-doc1 (:id tpl-doc))]
    (doseq [doc [md-doc media-doc tpl-doc test-doc]]
      (save-document doc))))


(defn reset-home
  []
  (let-realised [doc (get-document :home)]
    ;(doseq [doc (conj (:modules @doc) @doc)]
    (delete-document @doc)
    ; (create-test-doc)
    ; (create-test-doc2)
    (create-home)))

