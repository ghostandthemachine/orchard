(ns think.objects.project-loader
  (:require [think.object :as object]
            [think.util.log :refer [log]]))

(def home* (atom nil))

(object/behavior* ::load-home-doc
                  :triggers #{:load-home}
                  :reaction (fn [this doc]
                              (log "Load home doc " doc)
                              (reset! home* doc)
                              (object/update! this [:projects] merge doc)))


(object/object* ::loader
                :triggers [:load-home]
                :behaviors [::load-home-doc]
                :projects {}
                :init (fn [this]))

(def loader (object/create ::loader))
