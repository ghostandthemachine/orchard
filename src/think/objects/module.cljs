(ns think.objects.module
  (:require [think.objects.modules.markdown :as markdown]))

(defn create
  [record]
  (case (:type record)
    (markdown/create record)))
