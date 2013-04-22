(ns think.objects.modules
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.model :as model]))


;; TODO: rather than using the (first (:args this)) to get the original record,
;; we should probably keep it around explicitly, or else just save the keys on load.
(object/behavior* ::save-module
                  :triggers #{:save}
                  :reaction (fn [this]
                              (log "save module: ")
                              (log-obj this)
                              (let [original-doc (first (:args @this))
                                    doc-keys     (keys original-doc)
                                    new-doc      (select-keys @this doc-keys)]

                                (log-obj original-doc)
                                (log-obj doc-keys)
                                (log-obj new-doc)
                                (model/save-document new-doc))))
