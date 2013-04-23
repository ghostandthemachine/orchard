(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :refer [uuid bound-do]]
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
  [:button.btn.btn-small.btn-primary.pull-right.add-module-btn
    [:i.icon-plus-sign.icon-white]]
  :click (fn [e]
            (object/update! this [:modules] concat (list (object/create :markdown-module {:text "#### new module" :id (uuid)})))))


(object/object* :single-column-template
                :triggers #{}
                :behaviors []
                :init (fn [this template-record document]
                          (let [module-objs (map
                                              #(object/create (keyword (:type %)) %)
                                              (:modules template-record))
                                new-tpl     (assoc template-record :modules module-objs)]
                            (object/merge! this new-tpl)
                            (bound-do (subatom this :modules) (fn [_] (object/raise document :save)))
                            [:div.template.single-column-template
                              [:div.fluid-row
                                (bound (subatom this :modules) (partial render-modules this))]
                              [:div.fluid-row
                                (add-module-btn this)]])))

