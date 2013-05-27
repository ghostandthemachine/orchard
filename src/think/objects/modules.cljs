(ns think.objects.modules
  (:use-macros [think.macros :only [defui]]
               [redlobster.macros :only [let-realised]])
  (:require [think.object :as object]
            [think.util.dom :as dom]
            [think.util.log :refer [log log-obj]]
            [crate.binding :refer [bound subatom]]
            [think.model :as model]))


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
                  (log-obj doc)
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
  [parent mod-a mod-b]
  (let [id-a (:id @mod-a)
        id-b (:id @mod-b)]
    (log (str "replace mods " id-a ", " id-b))

    (object/update! parent [:modules]
      #(reduce
        (fn [mods mod]
          (conj mods
            (if (= (:id @mod) id-a)
              mod-b
              mod)))
        []
        %))))


(defui render-icon
  [selector icon create-fn doc]
  [:div.module-selector-icon
    icon]
  :click (fn [e]
          (log "save new mod ")
          (log-obj doc)
          (let-realised [new-doc (model/save-document doc)]
            (log "saved new mod - new-doc: ")
            (log-obj @new-doc)
            (let [parent  (object/parent selector)
                  new-mod (create-fn doc)]
              (object/parent! parent new-mod)
              (replace-module parent selector new-mod)))))


