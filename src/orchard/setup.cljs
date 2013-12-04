(ns orchard.setup
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
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
  (log "checking for the home project...")
  (go
    (let [obj (<! (model/get-object db :home))]
      (log "home: [" obj "]")
      (if obj
        (log "Found home project.")
        (do
          (log "Creating home project...")
          (save-home-project! db))))))