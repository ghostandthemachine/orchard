(ns orchard.setup
  (:require [orchard.util.core :as util]
            [orchard.model :refer [save-document]]))


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
   :text "<button class=\"btn\" onclick=\"orchard.objects.new$.load_new_doc();\">New</button>"
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
   :title "orchard app"})


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


; (defn reset-home
;   []
;   (let-realised [doc (get-document :home)]
;     ;(doseq [doc (conj (:modules @doc) @doc)]
;     (delete-document @doc)
;     ; (create-test-doc)
;     ; (create-test-doc2)
;     (create-home)))
