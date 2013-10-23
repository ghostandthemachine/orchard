(ns orchard.module
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [cljs.core.async      :refer (chan >! <! timeout)]
    [orchard.util.log     :refer (log log-obj)]))


(defprotocol ModuleTransaction
  (save-module! [parent object]
    "Saves {:id value}.")

  (add-module [parent object]
    "Returns the object on a channel.")

  (remove-module [parent object]
    "Returns a seq of all objects on a channel.")

  (move-module [parent source-index target-index]
    "Swap position order of a module in a parent"))