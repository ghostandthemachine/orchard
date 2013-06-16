(ns think.objects.nav
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util.core :as util]
            [think.objects.workspace :as workspace]
            [think.util.dom :as dom]
            [think.model :as model]
            [crate.binding :refer [map-bound bound subatom]]
            [think.model :as model]
            [redlobster.promise :as p]))


(defn wiki-doc
  []
  (:wiki-document @workspace/workspace))


(defn locked?
  [wiki-doc]
  (:locked? @wiki-doc))


(defn toggle-lock!
  [wiki-doc]
  (object/update! wiki-doc [:locked?]
    (fn [b] (not b))))


(defui new-doc-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-plus.icon-white.nav-icon]]
  :click #(think.objects.new/load-new-doc))


(defui home-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-home.icon-white.nav-icon]]
  :click (fn [e]
            (think.objects.app/open-document :home)))


(defn lock-handler
  [this e]
  (let [lock (not (:locked? @this))]
    (object/assoc! this :locked? lock)
    (object/raise (wiki-doc)
      (if lock
        :lock-document
        :unlock-document))))


(defui lock-view
  [this]
  [:i#lock-btn.icon-lock.icon-white.nav-icon]
  :click (partial lock-handler this))


(defui unlock-view
  [this]
  [:i#lock-btn.icon-unlock.icon-white.nav-icon]
  :click (partial lock-handler this))


(defn handle-lock-btn
  [this locked?]
  (let [btn$ (js/$ "#lock-btn")]
    (if (:locked? @this)
      (lock-view this)
      (unlock-view this))))


(defn lock-btn
  [this]
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    (bound (subatom this [:locked?]) (partial handle-lock-btn this))])


(defui refresh-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-refresh.icon-white.nav-icon]]
  :click (fn [e]
            (object/raise think.objects.app/app :refresh)))


(defui synch-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-sitemap.icon-white.nav-icon]]
  :click (fn [e]
            (log "synch projects")
            (let-realised [p (model/synch-documents)]
              (log "Documents synched")
              (log-obj @p))))


(defui dev-tools-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-dashboard.icon-white.nav-icon]]
  :click (fn [e]
            (object/raise think.objects.app/app :show-dev-tools)))


(defn text-input
  []
  [:input.nav-input {:type "text"}])




(object/behavior* ::add!
  :triggers #{:add!}
  :reaction (fn [this]
              (log "Add nav to workspace")
              (dom/append (dom/$ "body") (:content @this))))



(object/object* :workspace-nav
  :triggers #{:add!}
  :behaviors [::add!]
  :locked? true
  :init (fn [this]
          [:div.top-nav
            [:div.nav-container.row-fluid
              [:div.span3.row
               (home-btn)
               (new-doc-btn)
               (refresh-btn)]
              [:div.span6.row (text-input)]
              [:div.span3.row
               (lock-btn this)
               (synch-btn)
               (dev-tools-btn)]]]))


(def workspace-nav (object/create :workspace-nav))

