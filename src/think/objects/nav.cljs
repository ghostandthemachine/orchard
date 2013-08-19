(ns think.objects.nav
  (:require-macros
    [think.macros :refer (defui defonce node-chan)]
    [cljs.core.async.macros :refer (go)])
  (:require
    [think.object    :as object]
    [cljs.core.async :refer [chan >! <!]]
    [think.util.log  :refer (log log-obj)]
    [think.util.core :as util]
    [think.util.dom  :as dom]
    [crate.binding   :refer [map-bound bound subatom]]
    [think.model     :as model]
    [think.dispatch  :as dispatch]))


(def BLOCK-SIZE 30)  ;; default nav (and sidebar btn) btn size

(defn wiki-doc
  []
  (:wiki-document @think.objects.workspace/workspace))


(defn locked?
  [wiki-doc]
  (:locked? @wiki-doc))


(defn toggle-lock!
  [wiki-doc]
  (object/update! wiki-doc [:locked?]
    (fn [b] (not b))))


(defui new-doc-btn
  []
  [:li.nav-element
    [:i.icon-plus.icon-white]]
  :click #(think.objects.new/load-new-doc))


(defui home-btn
  []
  [:li.nav-element
    [:i.icon-home.icon-white]]
  :click (fn [e]
            (think.objects.app/open-document :home)))


(defn lock-handler
  [this e]
  (let [lock (not (:locked? @this))]
    (object/assoc! this :locked? lock)
    (object/raise (wiki-doc)
      (if lock
        :lock-document
        :unlock-document))
    (.preventDefault e)
    false))


(defui lock-view
  [this]
  [:i#lock-btn.icon-lock.icon-white]
  :mousedown (fn [& args] false)
  :click (partial lock-handler this))


(defui unlock-view
  [this]
  [:i#lock-btn.icon-unlock.icon-white]
  :click (partial lock-handler this))


(defn lock-btn-handler
  [this locked?]
  (let [btn$ (js/$ "#lock-btn")]
    (if (:locked? @this)
      (lock-view this)
      (unlock-view this))))


(defn lock-btn
  [this]
  [:li.nav-element
    (bound (subatom this [:locked?]) (partial lock-btn-handler this))])


(defui refresh-btn
  []
  [:li.nav-element
    [:i.icon-refresh.icon-white]]
  :click (fn [e]
            (object/raise think.objects.app/app :refresh)))


(defui synch-btn
  []
  [:li.nav-element
   [:i.icon-sitemap.icon-white]]
  :click (fn [e]
           (log "synch projects")
           (go
             (<! (model/synch-documents))
             (log "Documents synched"))))


(defui dev-tools-btn
  []
  [:li.nav-element
    [:i.icon-dashboard.icon-white]]
  :click (fn [e]
            (object/raise think.objects.app/app :show-dev-tools)))


(defn accum-btn-widths
  []
  (apply + (map #(aget % "offsetWidth") (dom/$$ ".nav-element"))))

(defn accum-padding
  [elem]
  (let [style (util/computed-style elem)]
    (+
      (util/parse-int (aget style "paddingLeft"))
      (util/parse-int (aget style "paddingRight")))))

(defn format-width
  [width]
  (let [bounds (or (accum-btn-widths) 0)]
    (- width 4
      (if (> 0 bounds)
        bounds
        180))))

(defn text-input
  [this]
  [:li.nav-input-element
    [:input#nav-input
      {:type "text"
       :style {:width (bound this #(format-width (:window-width @this)))}}]])


(object/behavior* ::add!
  :triggers #{:add!}
  :reaction (fn [this]
              (dom/append (dom/$ "body") (:content @this))))


(object/object* :workspace-nav
  :triggers #{:add!}
  :behaviors [::add!]
  :locked? true
  :window-width (dom/window-width)
  :init (fn [this]
          [:div#nav-wrapper
            [:ul#nav-list
              (home-btn)
              (new-doc-btn)
              (refresh-btn)
              (text-input this)
              (lock-btn this)
              (synch-btn)
              (dev-tools-btn)]]))


(def workspace-nav (object/create :workspace-nav))


(dispatch/react-to #{:resize-window}
  (fn [ev & [e]]
    (object/assoc! workspace-nav :window-width
      (aget (aget e "target") "innerWidth"))))

