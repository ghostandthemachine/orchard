(ns think.objects.modules.visualizer
  (:use-macros [dommy.macros :only [sel]]
               [think.macros :only [defui]])
  (:require 
            [think.object :as object]
            [think.util.core :refer [uuid]]
            [think.module :refer [module-btn-icon module-btn]]))


; (defn visualizer-doc
;   []
;   {:type :visualizer-module
;    :text "<h3>HTML here... </h3>"
;    :id   (uuid)})


; (def render-visual
;   (let [width 500, bar-height 20
;         data {"A" 1, "B" 2, "C" 4, "D" 3}
;         s (scale/linear :domain [0 (apply max (vals data))]
;                         :range [0 width])]
;     [:div#bars
;       (unify data (fn [[label val]]
;                    [:div {:style {:height bar-height
;                                   :width (s val)
;                                   :background-color "gray"}}
;                     [:span {:style {:color "white"}} label]]))]))


; (def icon [:span.btn.btn-primary.visualizer-icon "ViZ"])


; (object/object* :visualizer-module
;   :triggers #{}
;   :tags #{:module}
;   :reactions []
;   :init (fn [this]
;           [:div.span12.module.visualizer-module {:id (str "visualizer-module-" (:id @this))}
;                         [:div.module-tray (module-btn this)]
;                         [:div.module-element render-visual]]))


; (defn create-module
;   [doc]
;   (object/create :visualizer-module doc))
