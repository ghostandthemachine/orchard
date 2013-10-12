(ns orchard.model
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [orchard.util.core    :as util]
    [orchard.util.time    :as time]
    [cljs.core.async      :refer (chan >! <! timeout)]
    [orchard.util.log     :refer (log log-obj)]
    [orchard.object       :as object]))


(defprotocol ObjectStore
  (save-object! [this id value]
    "Saves {:id value}.")

  (get-object [this id]
    "Returns the object on a channel.")

  (all-objects [this]
    "Returns a seq of all objects on a channel."))

(defprotocol ObjectIndex
  (objects-of-type [this obj-type]
    "Returns a seq of objects of the given type."))

;  (index-object [this id value]
;    "Add this object to the index.")
;
;  (lookup [this index obj-key]
;    "Returns a channel of modules which ")
;
;  (module-by [this index k]
;    "Returns a channel of modules for which (pred module) => true"))


(defn load-object
  [db id]
  (go
    (let [doc (<! (get-object db id))]
      (when doc
        (let [obj-type (keyword (:type doc))
              doc (assoc doc :db db)]
          (if (object/defined? obj-type)
            (object/create obj-type doc)
            (do
              (log "load-object - type not found: " (str obj-type))
              :no-matching-page-type)))))))


(defn all-wiki-pages
  [db]
  (go (<! (objects-of-type db :wiki-page))))


(defn all-projects
  [db]
  (go (<! (objects-of-type db :project))))


(defn all-pages-by-title
  [db]
  (go
    (let [docs (<! (all-wiki-pages db))]
      (sort-by :title docs))))


(defn module
  [mod-type]
  {:type       mod-type
   :id         (util/uuid)
   :created-at (util/date-json)
   :updated-at (util/date-json)})


(defn project
  "Create a new project with a title."
  [{:keys [title root] :as project}]
  (merge project (module :project)))

(defn wiki-page
  "Create a new wiki page with the given template, title and revision number."
  [{:keys [title template] :as page}]
  (merge page (module :wiki-page)))


(defn single-column-template
  [& mod-ids]
  (assoc (module :single-column-template)
         :modules mod-ids))


(defn index-module
  []
  (module :index-module))


(defn html-module
  []
  (assoc (module :html-module)
         :text ""))


(defn markdown-module
  []
  (assoc (module :markdown-module)
         :text ""))


(defn media-module
  ([] (media-module {}))
  ([{:keys [title authors path filename notes annotations cites tags] :as doc}]
   (merge doc (module :media-page))))


