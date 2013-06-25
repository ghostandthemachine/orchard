(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util.core :as util]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.module :refer [top-spacer spacer]]
            [think.objects.modules.module-selector :as selector]
            [crate.binding :refer [map-bound bound subatom]]
            [think.model :as model]
            [redlobster.promise :as p]))


(object/behavior* ::ready
  :triggers #{:ready}
  :reaction (fn [this id] ))


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
              (let [mod-ids      (map #(:id @%)
                                   (filter
                                     #(not= (:type @%) "module-selector-module") (:modules @this)))
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
                (log "saving template document...")
                (let [doc (model/save-document new-doc)]
                  (p/on-realised doc
                  	(fn []
                        (let [rev (:rev @doc)]
                          (log "done [rev = " rev "]")
                          (object/assoc! this :rev rev)))
                  	(fn [err]
                      (log "error loading doc " err)
                      (log "initial rev: " (:rev (first (:args @this))))
                      (log "current rev: " (:rev @this))))))))


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
              (object/raise this :save)
              (.css (js/$ ".module")
  							"background-color"
  							"rgb(247, 247, 247)")))


(object/object* :single-column-template
  :triggers #{:save :ready :remove-module :add-module}
  :tags #{:template}
  :behaviors [::save-template ::ready ::remove-module ::add-module]
  :init (fn [this tpl]
          (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
            (let [module-objs (map
                                #(object/create
                                   (keyword (:type %)) %)
                                @mods)
                  new-tpl     (assoc tpl :modules module-objs)]
              (object/merge! this new-tpl)
              (doseq [mod module-objs]
                (object/parent! this mod))))
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

