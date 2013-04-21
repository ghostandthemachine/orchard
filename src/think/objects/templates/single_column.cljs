
(ns think.objects.templates.single-column
  (:require [think.object :as object]
            [think.objects.module :as module]
            [think.util.log :refer [log]]
            [crate.binding :refer [map-bound bound subatom]]))


(defn render-modules
  [this modules]
  [:div.span12
    (for [module modules]
      (:content module))])

(def t (atom nil))

(object/object* ::single-column
                :triggers #{}
                :behaviors []
                :init (fn [this template-record]
                          (let [module-objs (map module/create (:modules template-record))
                                new-tpl     (assoc template-record :modules module-objs)]
                            (object/merge! this new-tpl)
                            [:div.container-fluid
                              [:h3 "template"]
                                (bound (subatom this [:modules]) (partial render-modules this))])))


(def single-column (object/create ::single-column))


(defn create
  [template-record]
  (object/create ::single-column template-record))