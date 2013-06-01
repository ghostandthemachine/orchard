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


(defui render-modules
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
              (let [mod-ids       (map #(:id @%)
                                    (filter
                                      #(not= (:type @%) "module-selector-module") (:modules @this)))
                    original-doc  (first (:args @this))
                    rev           (:rev original-doc)
                    id            (:id original-doc)
                    doc-keys      (into []
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
                    								:rev     (:rev original-doc)
                    								:id      (:id original-doc))]
                (let-realised [doc (model/save-document new-doc)]
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
              (object/parent! template new-mod)
              (object/update! template [:modules] #(insert-at % index new-mod))
              (.css (js/$ ".module")
  							"background-color"
  							"rgb(247, 247, 247)")))


(object/object* :single-column-template
  :triggers #{:save :post-init :remove-module :add-module}
  :tags #{:template}
  :behaviors [::save-template ::post-init ::remove-module ::add-module]
  :init (fn [this tpl]
  				(log "init new single column template")
          (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
            (let [module-objs (map
                                #(object/create
                                  (keyword (:type %)) %)
                                @mods)
                  new-tpl     (assoc tpl :modules module-objs)]
              (object/merge! this new-tpl)
              (log "new merged tempalte")
              (doseq [mod module-objs]
                (object/parent! this mod))))
         	(util/bound-do (subatom this :modules)
						(fn [mods]
						  (log "update single column template modules")
						  (object/raise this :save)))
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
