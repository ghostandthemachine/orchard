(ns think.objects.synch-btn
  (:use-macros [think.macros :only [defui defgui]]
               [dommy.macros :only [sel sel1]]
               [redlobster.macros :only [let-realised when-realised]])
  (:require [think.object :as object]
            [think.model :as model]
            [crate.binding :refer [subatom bound]]
            [think.util.log :refer (log log-obj)]
            [think.util.dom :as dom]))


; (def account (js->clj js/account))


; (def p (replicate-db "projects" "http://jon:celerycatstick@lifeisagraph.com:5984/projects"))



(defui btn-view
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-refresh.icon-white.nav-icon]]
  :click (fn [e]
            (log "synch projects")
            (log-obj js/account)))


(object/object* :synch-btn
  :triggers #{}
  :behaviors []
  :init (fn [this]
          (btn-view)))


(def synch-btn (object/create :synch-btn))