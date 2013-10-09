(ns orchard.setup
  (:require-macros
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require [orchard.util.core :as util]
            [orchard.util.log :refer (log log-obj log-err)]
            [orchard.model     :as model]
            [cljs.core.async   :refer (chan >! <! timeout)]))


(defn save-home-project!
  "Creates the default home project, saving all enclosed objects in the database."
  [db]
  (let [index     (model/index-module)
        html      (model/html-module)
        tpl       (model/single-column-template (:id index) (:id html))
        page      (model/wiki-page {:title "Home" :template (:id tpl)})
        project   (model/project  {:title "Home" :root (:id page)})
        project   (assoc project :id :home)]
    (doseq [obj [index html tpl page project]]
      (model/save-object! db (:id obj) obj))
    project))


(defn check-home
  "Checks to see if the :home project exists, creating it if not."
  [db]
  (go
    (log "Checking home...")
    (let [doc (<! (model/get-object db :home))]
      (log "doc returned for home")
      (log-obj doc)
      (if (nil? doc)
        (do
          (log "Creating home project...")
          (save-home-project! db))
        (do
          (log "Found home project."))))))


(defn test-doc
  [id tpl-id]
  (assoc (model/wiki-page
           {:title (str "test-doc[" id "]")
            :template tpl-id})
         :id id))


(defn create-test-doc
  [db]
  (let [md-doc     (model/markdown-module)
        ht-doc     (model/html-module)
        tpl-doc    (model/single-column-template (:id md-doc) (:id ht-doc))
        test-doc   (test-doc :test-doc1 (:id tpl-doc))]
    (doseq [doc [md-doc ht-doc tpl-doc test-doc]]
      (model/save-object! db (:id doc) doc))))


(defn create-media-doc
  [db]
  (let [md-doc     (model/markdown-module)
        media-doc  (model/media-module)
        tpl-doc    (model/single-column-template (:id md-doc) (:id media-doc))
        test-doc   (test-doc :test-doc1 (:id tpl-doc))]
    (doseq [doc [md-doc media-doc tpl-doc test-doc]]
      (model/save-object! db (:id doc) doc))))


