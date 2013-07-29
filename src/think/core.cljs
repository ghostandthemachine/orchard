(ns think.core
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :as dom]
            [think.util.core :refer [log log-obj]]))