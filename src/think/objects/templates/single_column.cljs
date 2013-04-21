
(ns think.objects.templates.single-column
  (:require [think.object :as object]
            [think.objects.module :as module]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [map-bound bound subatom]]))


(defn render-modules
  [this modules]
  [:div.span12
    (for [module modules]
      (:content @module))])

(object/object* :single-column-template
                :triggers #{}
                :behaviors []
                :init (fn [this template-record]
                          (let [module-objs (map
                                              #(object/create (keyword (:type %)) %)
                                              (:modules template-record))
                                new-tpl     (assoc template-record :modules module-objs)]
                            (object/merge! this new-tpl)
                            [:div.container-fluid
                              (bound (subatom this [:modules]) (partial render-modules this))])))

