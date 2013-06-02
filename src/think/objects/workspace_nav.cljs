(ns think.objects.workspace-nav
  (:use-macros [think.macros :only [defui defgui]]
               [dommy.macros :only [sel sel1]]
               [redlobster.macros :only [let-realised when-realised]])
  (:require [think.object :as object]
            [think.model :as model]
            [crate.binding :refer [subatom bound]]
            [think.util.log :refer [log log-obj]]
            [think.util.dom :as dom]))


(defn new-doc-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-plus.icon-white.nav-icon]])


(defn home-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-home.icon-white.nav-icon]])


(defui lock-btn
  [nav]
  [:span.btn.btn-small.btn-nav-dark.nav-btn.pull-right
    [:i.lock-btn.icon-lock.icon-white.nav-icon]]
  :click (fn [e]
            (this-as btn
              (let [icon (sel1 btn ".lock-btn")]
                (log "iebwhbgwhu")
                (log btn)))))


(object/object* :workspace-nav
  :triggers #{}
  :behaviors []
  :init (fn [this]
          [:div.top-nav
            [:div.nav-container
              (home-btn)
              (new-doc-btn)
              (lock-btn this)]]))


(def workspace-nav (object/create :workspace-nav))

