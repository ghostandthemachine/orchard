
(ns think.objects.templates.single-column
  (:require [think.object :as object]
            [think.objects.module :as module]
            [crate.binding :refer [map-bound bound subatom]]))


(defn render-modules
  [this modules]
  [:div.span12
    ; (for [[_ module] modules]
    ;   (:content module))

    ])

(def t (atom nil))

(object/object* ::single-column
                :triggers #{}
                :behaviors []
                :init (fn [this template-record]
                        (object/merge! this
                          (assoc template-record :modules (map module/create (:modules template-record))))
                        (reset! t template-record)

                        [:div.container-fluid
                          (bound (subatom this [:modules]) (partial render-modules this))]))


(def single-column (object/create ::single-column))


(defn create
  [template-record]
  (object/create ::single-column template-record))