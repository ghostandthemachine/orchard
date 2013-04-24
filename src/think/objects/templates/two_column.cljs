(ns think.objects.templates.two-column
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :refer [uuid]]
            [crate.binding :refer [map-bound bound subatom]]))


; (defn render-modules
;   [this modules]
;   [:div.modules
;     (for [module modules]
;       [:div.row-fluid
;         (:content @module)])])


; (defui add-module-btn
;   [this k]
;   [:button.btn.btn-mini.btn-primary.pull-right.add-module-btn
;     [:h4 "+"]]
;   :click #(object/update! this [k] concat (list (object/create :markdown-module {:text "#### new module" :id (uuid)}))))


; (object/object* :two-column-template
;                 :triggers #{}
;                 :behaviors []
;                 :init (fn [this template-record]
;                           (let [module-objs (map
;                                               #(object/create (keyword (:type %)) %)
;                                               (:modules template-record))
;                                 new-tpl     (assoc template-record :modules-left module-objs :modules-right [])]
;                             (object/merge! this new-tpl)
;                             [:div.template.two-column-template
;                               [:div.fluid-row
;                                 [:span6
;                                   [:div.fluid-row
;                                     (bound (subatom this [:modules-left]) (partial render-modules this))]
;                                   [:div.fluid-row
;                                     (add-module-btn this :modules-left)]]
;                                 [:span6
;                                   [:div.fluid-row
;                                     (bound (subatom this [:modules-right]) (partial render-modules this))]
;                                   [:div.fluid-row
;                                     (add-module-btn this :modules-right)]]]])))

