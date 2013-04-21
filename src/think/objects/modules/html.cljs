(ns think.objects.modules.html
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [crate.core :as crate]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]))

(defui render-html
  [this text]
  [:div.html-module-content
    (crate/raw text)])

(object/object* :html-module
                :tags #{}
                :mode :present
                :init (fn [this record]
                        (object/merge! this record)
                        [:div.module.html-module
                          (bound (subatom this [:text]) (partial render-html this))]))
