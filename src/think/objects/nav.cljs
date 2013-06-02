(ns think.objects.nav
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [when-realised let-realised defer-node]])
  (:require [think.object :as object]
            [think.util.log :refer [log log-obj]]
            [think.util :as util]
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


(defn new-doc-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-plus.icon-white.nav-icon]])


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
    (log-obj btn$)
    (if (:locked? @this)
      (lock-view this)
      (unlock-view this))))


(defn lock-btn
  [this]
  [:span.btn.btn-small.btn-nav-dark.nav-btn.lock-btn.pull-right
    (bound (subatom this [:locked?]) (partial handle-lock-btn this))])


(defui synch-btn
  []
  [:span.btn.btn-small.btn-nav-dark.nav-btn
    [:i.icon-refresh.icon-white.nav-icon]]
  :click (fn [e]
            (log "synch projects")
            (let-realised [p (model/synch-documents)]
              (log "Documents synched")
              (log-obj @p))))


(defn text-input
  []
  [:input.span4.nav-input {:type "text"}])


(object/object* :workspace-nav
  :triggers #{}
  :behaviors []
  :locked? true
  :init (fn [this]
          [:div.top-nav
            [:div.nav-container
              (home-btn)
              (new-doc-btn)
              (synch-btn)
              (text-input)
              (lock-btn this)]]))


(def workspace-nav (object/create :workspace-nav))

