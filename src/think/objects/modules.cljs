(ns think.objects.modules
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.util.dom :as dom]
            [crate.core :as crate]
            [think.util.log :refer [log log-obj]]
            [think.util :refer [index-of]]
            [crate.binding :refer [bound subatom]]
            [think.model :as model]))



(defn index-of-module
  [module]
  (log-obj (into [] (map #(:id @%)
      (:modules @(object/parent module)))))
  (log-obj @(object/parent module))
  (index-of
    (:id @module)
    (map #(:id @%)
      (:modules @(object/parent module)))))


(def default-opts
  (clj->js
    {:mode "markdown"
     :theme "default"
     :lineNumbers true
     :tabMode "indent"
     :autofocus true
     :linewrapping true
     :matchBrackets true
     :viewportMargin js/Infinity}))


(defn edit-module-btn-icon
  [mode]
  (if (= mode :present)
    "icon-pencil module-btn"
    "icon-ok module-btn"))


(defn $module
  [this]
  (dom/$ (str "#module-" (:id @this) " .module-content")))


(defui delete-btn
  [this]
  [:i.icon-trash.module-btn]
  :click (fn [e]
            (log "Delete module btn hit " (:id @this))
            (log-obj @this)
            (let [msg "Are you sure you want to delete this module?"
                   delete? (js/confirm msg)]
                (when delete?
                  (object/raise (object/parent this) :remove-module this)))))

(defui edit-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) edit-module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))



;; TODO: rather than using the (first (:args this)) to get the original record,
;; we should probably keep it around explicitly, or else just save the keys on load.
(object/behavior* ::save-module
  :triggers #{:save}
  :reaction (fn [this]
              (log "Save module")
              (let [original-doc (first (:args @this))
                    doc-keys     (conj (keys original-doc) :rev)
                    new-doc      (select-keys @this doc-keys)]
                (let-realised [doc (model/save-document new-doc)]
                  (log "save handler...")
                  (log-obj @doc)
                  (object/assoc! this :rev (:rev @doc))))))


(defn swap-modules
  [parent mod-a mod-b]
  (when (= (count (some #{(:id @mod-a) (:id @mod-b)} (:modules @parent))) 2)
    (let [id-a (:id @mod-a)
          id-b (:id @mod-b)]
      (object/update! parent [:modules]
        #(reduce
          (fn [mods mod]
            (conj mods
              (case (:id mod)
                id-a mod-b
                id-b mod-a
                mod)))
          %)))))


(defn replace-module
  [old-mod new-mod]
  (let [new-id (:id @old-mod)
        old-id (:id @new-mod)
        parent (object/parent old-mod)
        mods   (reduce
                  (fn [mods mod]
                    (conj mods
                      (if (= (:id @mod) new-id)
                        new-mod
                        mod)))
                  []
                  (:modules @parent))]
    (object/assoc! parent :modules mods)
    (object/parent! parent new-mod)))



(defn insert-at [coll pos item]
  (let [vec (into [] coll)]
    (apply merge (subvec vec 0 pos) item (subvec vec pos))))


(defui create-module-icon
  [template index icon create-fn]
  [:div.module-selector-icon
    icon]
  :click (fn [e]
          (let-realised [mod (create-fn)]
            (log "adding new module")
            (log-obj @mod)
            (log-obj template)
            (object/raise template :add-module template @mod index))))


(defn module-btn-icon
  [mode]
  (if (= mode :present)
    "icon-pencil module-btn"
    "icon-ok module-btn"))


(defui module-btn
  [this]
  [:i {:class (bound (subatom this [:mode]) module-btn-icon)}]
  :click (fn [e]
            (object/assoc! this :mode
              (if (= (:mode @this) :present)
                :edit
                :present))))


(defn popover
  [module]
  (let [index (index-of-module module)
        template (object/parent module)]
    [:div.container.add-module-popover
      (for [icon [(create-module-icon template index
                    think.objects.modules.markdown/icon
                    think.objects.modules.markdown/create-module)
                  (create-module-icon template index
                    think.objects.modules.html/icon
                    think.objects.modules.html/create-module)]]
        [:div.row-fluid
          [:div.span2
            icon]])]))


(def clicked-away* (atom false))
(def is-visible*   (atom false))


(.click (js/$ js/document)
  (fn [e]
    (if (and @is-visible* @clicked-away*)
      (do
        (.popover (js/$ ".popover-trigger") "hide")
        (reset! clicked-away*
          (reset! is-visible* false)))
      (reset! clicked-away* true))))


(defui create-module-btn
  [prev-mod]
  [:span.btn.btn-mini.btn-primary.popover-trigger
    {:data-title     "Add New Module"
     :data-placement "right"
     :data-html      "true"
     :data-trigger   "manual"}
    "add"]
  :click (fn [e]
          (this-as this
            (let [popover-html (crate/html (popover prev-mod))]
              (.popover (js/$ this)
                (clj->js {:content popover-html}))
              (.popover (js/$ this) "show")
              (reset! clicked-away* false)
              (reset! is-visible* true)
              (.preventDefault e)))))


(defn spacer-nav$
  [module]
  (js/$ (str "#spacer-nav-" (:id @module))))


(defui spacer
  [module]
  [:div.row-fluid.spacer-tray
    [:ul.spacer-nav {:id (str "spacer-nav-" (:id @module))}
      [:li.active.spacer-item
        (create-module-btn module)]]]
  :mouseover (fn []
              (.show (spacer-nav$ module)))
  :mouseout  (fn []
              (.hide (spacer-nav$ module))))

