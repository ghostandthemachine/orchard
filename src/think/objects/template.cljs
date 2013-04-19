(ns think.objects.single-column-template
  (:require [think.object :as object]
            [crate.binding :refer [map-bound bound subatom]]))


(defn template-modules
  [this modules]
  [:div.single-column-template
    (for [[_ module] modules]
      (:content module))])

(object/object* ::single-column-template
                :triggers #{}
                :behaviors []
                :init (fn [this]
                        (bound (subatom this [:modules]) (partial template-modules this))))


(def single-column-template (object/create ::single-column-template))

(defn record->template
  [record]
  (object/merge! single-column-template record))