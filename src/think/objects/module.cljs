(ns think.objects.module
  (:refer-clojure :exclude [val replace range])
  (:require [crate.core :as crate]
            [think.objects.context :as ctx-obj]
            [think.object :as object]
            [think.util.events :as ev])
  (:use [think.util.dom :only [remove-class add-class]]
        [think.object :only [object* behavior*]]
        [think.util.cljs :only [clj->js js->clj]]))

; (defui present-ui
;   [_ module]
;   (let [text (:text module)
;         html (js/markdown.toHTML text)]
;     [:div.present-markdown
;       (tpl/html->nodes html)]))



(object/object* ::module
                :tiggers #{}
                :behvaiors #{}
                :mode :present
                :init (fn [this]
                            [:div.module
                              [:h1 "wjrngkwrjgnwrk"]]))

(def module (object/create ::module))


(defn create-module
  [record]
  (let [module (object/create ::module)]
    (object/merge! module record)))



