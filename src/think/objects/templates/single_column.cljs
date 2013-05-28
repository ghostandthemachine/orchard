(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :as util]
            [think.util.dom :as dom]
            [think.model :as model]
            [think.objects.modules.module-selector :as selector]
            [crate.binding :refer [map-bound bound subatom]]
            [think.model :as model]))


(defn render-modules
  [this modules]
  [:ul.modules.connected-sortable {:id (str "sortable-" (:id @this))}
    (for [module modules]
      [:li.row-fluid.modules-item
        (:content @module)])])




(defui delete-btn
  [this]
  [:button.btn.btn-small.btn-primary.pull-right.delete-btn
    [:i.icon-trash.icon-white]]
  :click (fn [e]
            (let [msg "Are you sure you want to delete this module?"
                  delete? (js/confirm msg)]
              (when delete?
                ; (dom/remove this)
                (log "Delete template ")
                (log-obj this)))))

(defui add-module-btn
  [this]
  [:button.btn.btn-small.btn-primary.pull-right.add-module-btn
    [:i.icon-plus-sign.icon-white]]
  :click (fn [e]
          (log "add module button")
          (object/parent! this selector/module-selector-module)
          (object/update! this [:modules] conj selector/module-selector-module)))


(defn insert-at [vec pos item]
  (apply merge (subvec vec 0 pos) item (subvec vec pos)))


(object/behavior* ::save-template
  :triggers #{:save}
  :reaction (fn [this]
              (log "saving template...")
              (log-obj @this)
              (let [mod-ids       (map #(:id @%)
                                    (filter
                                      #(not= (:type @%) "module-selector-module") (:modules @this)))
                    original-doc  (first (:args @this))
                    doc-keys      (keys original-doc)
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

  ; $(function() {
    ; $( "#sortable-0c07772f-0b9c-4c90-92f4-fb27264f7e1e-wweg").sortable({
    ;   connectWith: ".connected-sortable"
    ; }).disableSelection();
  ; });

(object/behavior* ::add-module
  :triggers #{:add-module}
  :reaction (fn [this new-mod index]
              (object/parent! this mod)
              (object/update! [:modules] insert-at index new-mod)))


(object/object* :single-column-template
  :triggers #{:save :post-init :remove-module}
  :tags #{:template}
  :behaviors [::save-template ::post-init ::remove-module]
  :init (fn [this tpl]
          (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
            (let [module-objs (map #(object/create (keyword (:type %)) %) @mods)
                  new-tpl     (assoc tpl :modules module-objs)]
              (object/merge! this new-tpl)
              (doseq [mod module-objs]
                (object/parent! this mod))
              (util/bound-do (subatom this :modules)
                (fn [mods]
                  (object/raise this :save)))))
          [:div.template.single-column-template
            [:div.modules-container
              (bound (subatom this :modules)
                (partial render-modules this))]
            [:div.fluid-row.template-tray
              [:div.item
                (delete-btn this)]
              [:div.item
                (add-module-btn this)]]]))


(defn single-column-template-doc
  [& mod-ids]
  (model/save-document
    {:type :single-column-template
     :modules mod-ids
     :id (util/uuid)}))
