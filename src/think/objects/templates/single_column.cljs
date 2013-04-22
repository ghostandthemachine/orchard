
(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :refer [uuid]]
            [crate.binding :refer [map-bound bound subatom]]))


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
  :click #(object/update! this [:modules] concat (list (object/create :markdown-module {:text "#### new module" :id (uuid)}))))


(object/object* :single-column-template
                :triggers #{}
                :behaviors []
                :init (fn [this template-record]
                          (let [module-objs (map
                                              #(object/create (keyword (:type %)) %)
                                              (:modules template-record))
                                new-tpl     (assoc template-record :modules module-objs)]
                            (object/merge! this new-tpl)
                            [:div.template.single-column-template
                              [:div.fluid-row
                                (bound (subatom this [:modules]) (partial render-modules this))]
                              [:div.fluid-row
                                (add-module-btn this)]])))

