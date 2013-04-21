
(ns think.objects.templates.single-column
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [map-bound bound subatom]]))


(defn render-modules
  [this modules]
  [:div.modules
    (for [module modules]
      [:div.row-fluid
        (:content @module)])])

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
                              (bound (subatom this [:modules]) (partial render-modules this))])))

