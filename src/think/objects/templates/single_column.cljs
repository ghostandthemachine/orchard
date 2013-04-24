(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :as util]
            [crate.binding :refer [map-bound bound subatom]]
            [think.model :as model]))


(defn render-modules
  [this modules]
  [:div.modules
   (for [module modules]
     [:div.row-fluid
      (:content @module)])])


(defui add-module-btn
  [this]
  [:button.btn.btn-mini.btn-primary.pull-right.add-module-btn
   [:h4 "+"]]
  :click #(object/update! this [:modules] cons
                          (object/create :markdown-module
                                         {:text "#### new module" :id (util/uuid)})))


(object/object* :single-column-template
                :triggers #{}
                :behaviors []
                :init
                (fn [this tpl document]
                  (log "creating single column template with modules: " (:modules tpl))

                  (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
                    (let [module-objs (map #(object/create (keyword (:type %)) %) @mods)
                          new-tpl     (assoc tpl :modules module-objs)]
                      (object/merge! this new-tpl)

                      (util/bound-do (subatom this :modules)
                                     (fn [_] (object/raise document :save)))))

                  [:div.template.single-column-template
                   [:div.fluid-row
                    (bound (subatom this :modules)
                           (partial render-modules this))]
                   [:div.fluid-row
                    (add-module-btn this)]]))

