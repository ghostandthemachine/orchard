(ns think.model
  (:refer-clojure  :exclude [create-node])
  (:use [think.util.log :only    [log log-obj]])
  (:use-macros [redlobster.macros :only [when-realised let-realised defer-node]]
               [think.macros :only [defui]])
  (:require [think.util         :as util]
            [think.dispatch     :refer [fire react-to]]
            [think.couchdb      :as db]
            [think.object       :as object]
            [redlobster.promise :as p]
            [dommy.template :as tpl]))




(def DB "projects")


(object/behavior* ::init-db
                  :triggers #{:init-db}
                  :reaction (fn [this]
                              (log "Initialize db.. ")
                              (let-realised [db-promise (db/open DB)]
                                (object/merge! this
                                  {:ready? true
                                   :document-db* @db-promise})
                                (fire :db-loaded))))


(object/object* ::model
                :triggers  #{:db-loaded  :init-db}
                :behaviors [::db-loaded ::init-db]
                :ready? false
                :init (fn [this]
                        (object/raise this :init-db)))


(def model (object/create ::model))


(defn delete-document
  [doc]
  (db/delete-doc (:document-db* @model) doc))


(defn save-document
  [doc]
  (log "save-document: ")
  (log-obj doc)
  (db/update-doc (:document-db* @model)
                 (if (and (contains? doc :rev) (nil? (:rev doc)))
                   (dissoc doc :rev)
                   doc)))


(comment defn docs-of-type
  [doc-type]
  (let [doc-type (if (keyword? doc-type) (name doc-type) (str doc-type))
        mapper (fn [doc]
                 (log-obj doc)
                 (when (= doc-type (:type (js->clj doc)))
                   (emit (.-:id doc) doc)))]
    (db/map-reduce (:document-db* @model) mapper)))

    ;(db/query (:document-db* @model) {:select "*" :where (str "type=" doc-type)})))


(defn get-document
  [id]
  (let [res-promise (p/promise)
        doc-promise (db/get-doc (:document-db* @model) id)]
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
        (log "realizing new document of type: " (:type doc))
        (log-obj doc)
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
  (let-realised [docs (db/all-docs (:document-db* @model))]
    (log (str (keys @docs)))
    (if (= (:total_rows @docs) 0)
      []
      (util/await (map #(db/get-doc (:document-db* @model) (:id %)) (:rows @docs))))))

(defn all-wiki-documents
  []
  (let-realised [docs (db/view (:document-db* @model) :index :wiki-documents)]
    (log (str (keys @docs)))
    (if (= (:total_rows @docs) 0)
      []
      (map #(assoc % :id (:_id %)) (map :value (:rows @docs))))))
      ;(util/await (map #(db/get-doc (:document-db* @model) (:id %)) (:rows @docs))))))


(defn delete-all-documents
  "Delete all documents."
  []
  (let-realised [docs (all-documents)]
    (util/await (map #(db/delete-doc (:document-db* @model) %) @docs))))


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
      (save-document doc))))

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

