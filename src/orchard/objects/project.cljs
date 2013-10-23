(ns orchard.objects.project
  (:require-macros
    [orchard.macros :refer [defui defgui]]
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [cljs.core.async :refer [chan >! <! timeout]]
    [orchard.object :as object]
    [orchard.model :as model]
    [orchard.util.core :as util]
    [orchard.util.dom :refer [set-frame-title]]
    [orchard.objects.templates.single-column :as single-column]
    [orchard.util.module :as modules]
    [orchard.dispatch :as dispatch]
    [crate.binding :refer [subatom bound]]
    [orchard.util.log :refer (log log-obj)]))


(object/object* :project
  :triggers #{:save :ready}
  :behaviors [::ready]
  :title ""
  :init (fn [this]
          (go
            (log "rendering project...")
            (log-obj @this)
            (let [root (model/load-object (:db @this) (:root @this))]))))

