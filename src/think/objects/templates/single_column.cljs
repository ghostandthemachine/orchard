(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :as util]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.objects.modules :refer [top-spacer spacer insert-at]]
            [think.objects.modules.module-selector :as selector]
            [crate.binding :refer [map-bound bound subatom]]
            [think.model :as model]))


(defn render-modules
  [this modules]
  [:ul.modules.connected-sortable {:id (str "sortable-" (:id @this))}
    [:li.modules-item
      (top-spacer this)]
    (for [module modules]
      [:li.modules-item
        [:div.row-fluid
          (:content @module)]
        (spacer module)])])


(object/behavior* ::save-template
  :triggers #{:save}
  :reaction (fn [this]
              (log "saving template...")
              (log-obj @this)
              (let [mod-ids       (map #(:id @%)
                                    (filter
                                      #(not= (:type @%) "module-selector-module") (:modules @this)))
                    original-doc  (first (:args @this))
                    doc-keys      (conj (keys original-doc) :id :rev)
                    new-doc       (assoc (select-keys @this doc-keys) :modules mod-ids)]
                (let-realised [doc (model/save-document new-doc)]
                  (log "realised doc returned...")
                  (log (:rev @doc))
                  (object/assoc! this :rev (:rev @doc))))))


(object/behavior* ::remove-module
  :triggers #{:remove-module}
  :reaction (fn [this child]
              (log "remove child")
              (object/update! this [:modules]
                (fn [mods]
                  (filter #(not= (:id @%) (:id @child)) mods)))))


(object/behavior* ::post-init
  :triggers #{:post-init}
  :reaction (fn [this id]
              ; (log "post-init id " id)
              ; (log "Init after creating template. Content " (str "#sortable-" id))
              ; (log-obj (js/$ (str "#sortable-" id)))
              ; (log
              ;   (-> (js/$ (str "#sortable-" id))
              ;     (.sortable (clj->js {:connectWith ".connected-sortable"}))))
              ))


(object/behavior* ::add-module
  :triggers #{:add-module}
  :reaction (fn [this template new-mod index]
              (log "single-column-template add module")
              (log-obj template)
              (log-obj @new-mod)
              (object/parent! template new-mod)
              (object/update! template [:modules] #(insert-at % index new-mod))
              ; (object/raise template :save)
              ))


(object/object* :single-column-template
  :triggers #{:save :post-init :remove-module :add-module}
  :tags #{:template}
  :behaviors [::save-template ::post-init ::remove-module ::add-module]
  :init (fn [this tpl]
          (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
            (let [module-objs (map
                                #(object/create
                                  (keyword (:type %)) %)
                                @mods)
                  new-tpl     (assoc tpl :modules module-objs)]
              (object/merge! this new-tpl)
              (doseq [mod module-objs]
                (object/parent! this mod))
              (util/bound-do (subatom this :modules)
                (fn [mods]
                  (log "update single column template modules")
                  (log-obj mods)
                  (object/raise this :save)))))
          [:div.template.single-column-template
            [:div.modules-container
              (bound (subatom this :modules)
                (partial render-modules this))]]))


(defn single-column-template-doc
  [& mod-ids]
  (model/save-document
    {:type :single-column-template
     :modules mod-ids
     :id (util/uuid)}))
