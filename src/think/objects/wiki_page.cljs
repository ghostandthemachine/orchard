(ns think.objects.wiki-page
  (:require [think.object :as object]
            [think.objects.canvas :as canvas]
            [think.objects.module :as module]
            [think.util.cljs :refer [->dottedkw]]
            [crate.binding :refer [map-bound bound subatom]])
  (:require-macros [think.macros :refer [defui]]))

(def default-width 950)

; (defui wiki-page-modules [this modules]
;   [:div.container-fluid
;    (for [[_ m] modules]
;      (:content @m))])


(defui render-template
  [this template]
  (sctmpl/record->template template))

(object/behavior* ::width!
                  :triggers #{:width!}
                  :throttle 5
                  :reaction (fn [this e]
                              (when-not (= 0 (.-clientX e))
                                (object/merge! modules/multi {:left (+ 0 (.-clientX e))})
                                (object/merge! this {:width (- (.-clientX e) 40)
                                                     :max-width (- (.-clientX e) 40)}))
                              ))

(object/behavior* ::open!
                  :triggers #{:open!}
                  :reaction (fn [this]
                              (object/merge! this {:width (:max-width @this)})
                              (object/merge! modules/multi {:left (+ (:max-width @this) 40)})))

(object/behavior* ::close!
                  :triggers #{:close!}
                  :reaction (fn [this]
                              (object/merge! modules/multi {:left 40})
                              (object/merge! this {:active nil
                                                   :transients (list)
                                                   :width 0})))

(object/behavior* ::module-toggled
                  :triggers #{:toggle}
                  :reaction (fn [this module {:keys [force? transient? soft?]}]
                              (if (or (not= module (:active @this))
                                      force?)
                                (do
                                  (object/merge! this {:active module
                                                       :prev (when transient?
                                                               (or (:prev @this ) (:active @this) :none))})
                                  (object/raise this :open!)
                                  (when-not soft?
                                    (object/raise module :focus!)))
                                (object/raise this :close!))))

(defn active-content [active]
  (when active
    (object/->content active)))

(defn ->width [width]
  (str (or width 0) "px"))

(object/object* ::wiki-page
                :triggers #{}
                :behaviors [::module-toggled ::width! ::open! ::close!]
                :width 0
                :transients '()
                :max-width default-width
                :init (fn [this]
                        [:div#wiki-page
                         [:div.content-wrapper {:style {:width (bound (subatom this :width) ->width)}}
                          [:div.content
                            (bound (subatom this [:template]) (partial render-template this))]]]))

(def wiki-page (object/create ::wiki-page))

(canvas/add! wiki-page)


(defn add-module [module]
  (object/update! wiki-page [:modules] conj module))

(defn record->module
  [record]
  (module/create-module record))

