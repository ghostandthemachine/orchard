(ns think.objects.templates.single-column
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :as util]
            [think.model :as model]
            [crate.binding :refer [map-bound bound subatom]]
            [think.model :as model]))


(defn render-modules
  [this modules]
  [:div.modules
    (for [module modules]
      [:div.row-fluid
        (:content @module)])])


(defn markdown-doc
  []
  {:type ::markdown-module
   :text "## Markdown module"
   :id   (util/uuid)})


(defui add-module-btn
  [this]
  [:button.btn.btn-small.btn-primary.pull-right.add-module-btn
    [:i.icon-plus-sign.icon-white]]
;  :click #(object/update! this [:modules] conj
;            (object/create :markdown-module {:text "#### new module" :id (util/uuid)})))
  :click (fn [e]
          (let [md-doc  (markdown-doc)
                new-mod (object/create :markdown-module md-doc)]
            (object/update! this [:modules]
              (fn [mods]
                (concat mods
                  (list new-mod)))))))


(object/object* :single-column-template
                :triggers #{}
                :behaviors []
                :init
                (fn [this tpl document]
                  (log "creating single column template with modules: " (:modules tpl))

                  (let-realised [mods (util/await (map model/get-document (:modules tpl)))]
                    (let [module-objs (map #(object/create (keyword (:type %)) %) @mods)
                          new-tpl     (assoc tpl :modules module-objs)]
                      (object/merge! this new-tpl)
                      (util/bound-do (subatom this :modules)
                        (fn [_]
                          (log "template modules save fired")
                          (object/raise document :save)))))

                  [:div.template.single-column-template
                   [:div.fluid-row
                    (bound (subatom this :modules)
                           (partial render-modules this))]
                   [:div.fluid-row
                    (add-module-btn this)]]))
