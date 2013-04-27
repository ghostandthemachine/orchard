(ns think.objects.templates.two-column
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :refer [uuid]]
            [crate.binding :refer [map-bound bound subatom]]))


(defn render-left-modules
  [this modules]
  [:div.modules
    (for [module (:left modules)]
      [:div.row-fluid
        (:content @module)])])

(defn render-right-modules
  [this modules]
  [:div.modules
    (for [module (:right modules)]
      [:div.row-fluid
        (:content @module)])])


(defui add-module-btn
  [this k]
  [:button.btn.btn-mini.btn-primary.pull-right.add-module-btn
    [:h4 "+"]]
  :click #(object/update! this [k] concat (list (object/create :markdown-module {:text "#### new module" :id (uuid)}))))


(object/object* :two-column-template
                :triggers #{}
                :behaviors []
                :init (fn [this template-record]


                  (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
                    (let [module-objs (map #(object/create (keyword (:type %)) %) @mods)
                          new-tpl     (assoc tpl :modules module-objs)]
                      (object/merge! this new-tpl)
                      (util/bound-do (subatom this :modules)
                        (fn [_]
                          (log "template modules save fired")
                          (object/raise document :save)))))

                          (let [module-objs (map #(object/create (keyword (:type %)) %) @mods)
                                new-tpl     (assoc tpl :modules module-objs)]
                            (object/merge! this new-tpl)
                            [:div.template.two-column-template
                              [:div.fluid-row
                                [:span6
                                  [:div.fluid-row
                                    (bound (subatom this [:modules]) (partial render-left-modules this))]
                                  [:div.fluid-row
                                    (add-module-btn this :modules)]]
                                [:span6
                                  [:div.fluid-row
                                    (bound (subatom this [:modules]) (partial render-right-modules this))]
                                  [:div.fluid-row
                                    (add-module-btn this :modules)]]]])))

