(ns orchard.objects.modules.module-selector
  (:require-macros
    [orchard.macros :refer [defui]])
  (:require 
    [orchard.object :as object]
    [crate.core :as crate]
    [orchard.util.dom :as dom]
    [orchard.model :as model]
    [orchard.util.module :refer [module-btn module-btn-icon]]
    [orchard.util.module :as modules]
    [orchard.util.core :refer [bound-do uuid]]
    [orchard.util.log :refer (log log-obj)]
    [crate.binding :refer [bound subatom]]
    [dommy.core :as dommy]))


; (object/object* :module-selector-module
;   :tags #{}
;   :triggers #{}
;   :behaviors []
;   :id (uuid)
;   :init (fn [this]
;           [:div.span12.module.module-selector-module {:id (str "module-" (:id @this))}
;             [:div.module-tray (module-btn this)]
;             [:div.module-content.module-selector-module-content
;               [:div.row
;                 (for [icon [
;                             ; (modules/create-module-icon this
;                             ;   md/icon
;                             ;   md/create-module)
;                             ; (modules/create-module-icon this
;                             ;   html/icon
;                             ;   html/create-module)
;                             ; (modules/create-module-icon this
;                             ;   media/icon
;                             ;   media/create-module)
;                             ; (modules/create-module-icon this
;                             ;   viz/icon
;                             ;   viz/create-module)
;                             ]]
;                   [:div.span1
;                     icon])]]]))


; (def module-selector-module (object/create :module-selector-module))

