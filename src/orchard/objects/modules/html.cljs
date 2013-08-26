(ns orchard.objects.modules.html
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [orchard.macros :refer [defui]])
  (:require
    [cljs.core.async :refer (chan >! <!)]
    [orchard.object    :as object]
    [crate.core :as crate]
    [orchard.util.dom :as dom]
    [orchard.model :as model]
    [orchard.util.core :refer [bound-do uuid]]
    [orchard.module :refer [module-view default-opts edit-module-btn-icon delete-btn edit-btn]]
    [orchard.util.log :refer (log log-obj)]
    [crate.binding :refer [bound subatom]]
    [dommy.core :as dommy]))


(defui render-present
  [this]
  [:div.module-content.html-module-content
    (bound (subatom this :text) #(crate/raw %))])


(defui render-edit
  []
  [:div.module-content.html-module-editor])

(def icon [:span.btn.btn-primary.html-icon "<html>"])


(defn render-module
  [this mode]
  (dom/replace-with (dom/$ (str "#module-" (:id @this) " .module-content"))
    (case mode
      :present (render-present this)
      :edit    (render-edit)))
  (if (= mode :edit)
    (let [cm (js/CodeMirror
              (fn [elem]
                (dom/append (dom/$ (str "#module-" (:id @this) " .module-content"))
                  elem))
              default-opts)]
      (object/assoc! this :editor cm)
      (.setValue cm (:text @this)))
    (object/assoc! this :text (.getValue (:editor @this)))))


(object/object* :html-module
  :tags #{:module}
  :triggers #{:save :delete}
  :behaviors [:orchard.module/save-module :orchard.module/delete-module]
  :mode :present
  :editor nil
  :icon icon
  :label "HTML"
  :init (fn [this record]
          (object/merge! this record)
          (bound-do (subatom this [:mode]) (partial render-module this))
          (bound-do (subatom this :text) (fn [_] (object/raise this :save)))
          (module-view this
            [:div.module-element (render-present this)])))


(defn html-doc
  []
  (model/save-document
    {:type :html-module
     :text "<h3>HTML here... </h3>"
     :id   (uuid)}))


(defn create-module
  []
  (go
    (let [doc (<! (html-doc))
          obj (object/create :html-module doc)]
      obj)))



(dommy/listen! [(dom/$ :body) :.html-module-content :a] :click
  (fn [e]
    (let [href  (.-href (.-target e))
          tags  (clojure.string/split href #"/")
          proj  (get tags (- (count tags) 2))
          title (last tags)
          href  (str proj "/" title)]
      (log "open document from href " href)
      (orchard.objects.app/open-from-link href)
      (.preventDefault e))))
