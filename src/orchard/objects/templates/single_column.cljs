(ns orchard.objects.templates.single-column
  (:require-macros
    [orchard.macros :refer [defui]]
     [cljs.core.async.macros :as m :refer [go alt!]])
  (:require
    [cljs.core.async :refer (chan >! <! close! timeout)]
    [orchard.object :as object]
    [orchard.util.log :refer (log log-obj)]
    [orchard.util.core :as util]
    [orchard.util.dom :as dom]
    [orchard.model :as model]
    [orchard.util.module :refer [top-spacer spacer]]
    [crate.binding :refer [map-bound bound subatom]]))


(object/behavior* ::ready
  :triggers #{:ready}
              (fn [this]
                (doseq [mod (:modules @this)]
                  (object/raise mod :ready))))


(defui render-modules
  [this modules]
  [:div.modules
    (for [module modules]
      (let [content (:content @module)]
        (when (:ready @module)
          (go
            (let [rc        (<! (:ready-chan @module))
                  ready-fn  (:ready @module)]
              (ready-fn module))))
        content))])


(object/behavior* ::save-template
  :triggers #{:save}
  :reaction
  (fn [this]
    (let [mod-ids      (map #(:id @%) (:modules @this))
          original-doc (first (:args @this))
          rev          (:rev original-doc)
          id           (:id original-doc)
          doc-keys     (into []
                             (distinct
                               (conj
                                 (keys original-doc)
                                 :id :rev)))
          orig-vals     (select-keys @this doc-keys)
          ;; TODO This should not need to be done so explicitly but
          ;; is not working without out associating the id and rev
          ;; specifically
          new-doc       (assoc orig-vals
                               :modules mod-ids
                               ;; get the most current rev,
                               :rev     (or (:rev @this) (:rev original-doc))
                               :id      (:id original-doc))]
      (go
        (let [doc (<! (model/save-object! orchard.objects.app.db (:id @this) new-doc))]
          (object/assoc! this :rev (:rev doc)))))))


(object/behavior* ::remove-module
  :triggers #{:remove-module}
  :reaction (fn [this child]
              (object/update! this [:modules]
                (fn [mods]
                  (filter #(not= (:id @%) (:id @child)) mods)))
              (object/raise this :save)))


(object/behavior* ::add-module
  :triggers #{:add-module}
  :reaction (fn [this template new-mod index]
              (object/parent! template new-mod)
              (object/update! template [:modules] #(util/insert-at % index new-mod))
              (object/raise this :save)))


(object/object* :single-column-template
  :triggers #{:save :ready :remove-module :add-module}
  :tags #{:template}
  :behaviors [::save-template ::ready ::remove-module ::add-module]
  :init (fn [this tpl]
          (doseq [ch (map #(model/get-object orchard.objects.app.db %1) (:modules tpl))]
            (go
              (let [mod-record (<! ch)
                    module (object/create (keyword (:type mod-record)) mod-record)]
                (object/parent! this module)
                (object/update! this [:modules] conj module))))
          [:div.template.single-column-template
            (bound (subatom this :modules)
                   (partial render-modules this))]))


(defn single-column-template-doc
  [db & modules]
  (let [id (util/uuid)
        mod-ids (map :id modules)
        template {:type :single-column-template
                  :modules mod-ids
                  :id id}]
    (log "create single-column-template")
    (log-obj template)
    (model/save-object! db id template)))