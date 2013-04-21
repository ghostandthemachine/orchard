(ns think.objects.template
  (:require [think.objects.templates.single-column :as sctpl]))


(defn create
  [template-record]
  (case (:type template-record)
    (sctpl/create template-record)))