(ns orchard.objects.nav
  (:require-macros
    [orchard.macros :refer (defui node-chan)]
    [cljs.core.async.macros :refer (go)])
  (:require
    [orchard.object    :as object]
    [cljs.core.async :refer [chan >! <!]]
    [orchard.util.log  :refer (log log-obj)]
    [orchard.util.core :as util]
    [orchard.util.dom  :as dom]
    [crate.binding   :refer [map-bound bound subatom]]
    [orchard.model     :as model]
    [orchard.dispatch  :as dispatch]
    [orchard.objects.new :as n]))


(def visible* (atom true))

(def BLOCK-SIZE 30)  ;; default nav (and sidebar btn) btn size


(defn $nav-input
  []
  (dom/$ "#nav-input"))


(defn wiki-page
  []
  (:wiki-page @orchard.objects.workspace/workspace))


(defn locked?
  [wiki-page]
  (:locked? @wiki-page))


(defn toggle-lock!
  [wiki-page]
  (object/update! wiki-page [:locked?]
    (fn [b] (not b))))


(defn set-mode
  [this mode]
  (object/assoc! this :mode mode))


(defui new-doc-btn
  []
  [:div.btn
    [:i.icon-plus.icon-white]]
  :click n/load-new-doc)


(defui home-btn
  []
  [:div.btn
    [:i.icon-home.icon-white]]

  :click (fn [e]
            (orchard.objects.app/show-project orchard.objects.app.db :home)))


(defn lock-handler
  [this e]
  (let [lock (not (:locked? @this))]
    (object/assoc! this :locked? lock)
    ;(object/raise (wiki-page )
      (if lock
        :lock-document
        :unlock-document))
    (.preventDefault e)
    false)


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
  [:div.btn
    (bound (subatom this [:locked?]) (partial lock-btn-handler this))])


(defui refresh-btn
  []
  [:div.btn
    [:i.icon-refresh.icon-white]]
  :click (fn [e]
            (object/raise orchard.objects.app/app :refresh)))


(defui synch-btn
  []
  [:div.btn
   [:i.icon-sitemap.icon-white]]
  :click (fn [e]
           (log "synch projects")
           (go
             (<! (model/synch-documents))
             (log "Documents synched"))))


(defui dev-tools-btn
  []
  [:div.btn
    [:i.icon-dashboard.icon-white]]
  :click (fn [e]
            (object/raise orchard.objects.app/app :show-dev-tools)))


(defn focus
  []
  (.focus ($nav-input)))


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
        178))))


(defui text-input
  [this evc]
  [:li.nav-input-element
    [:input#nav-input
      {:type "text"
       :style {:width (bound this #(format-width (:window-width @this)))}}]]
  :keypress
    (fn [e]
      (go (>! evc e)))
  :keydown
    (fn [e]
      (when (= (.-keyCode e) 27)
        (go
          (>! evc e)))))


(defn set-input-text
  [s]
  (aset ($nav-input) "value" s))

(defn get-input-text
  []
  (aget ($nav-input) "value"))


(declare show-navbar)
(declare hide-navbar)

(declare workspace-nav)

(defn transient?
  ([]
    (transient? workspace-nav))
  ([this]
    (:transient? @this)))

(defn visible?
  []
  @visible*)


(defn focus-workspace
  []
  (.focus
    (:content @orchard.objects.workspace/workspace)))


(defn clean-up
  []
  (set-input-text "")
  (focus-workspace)
  (when (transient?)
    (hide-navbar)))


(defn build-document
  [db project-title document-title]
  (go
    (let [mod-doc    (<! (orchard.objects.modules.editor/editor-doc db))
          tpl-doc    (<! (orchard.objects.templates.single-column/single-column-template-doc db (:id mod-doc)))
          wiki-page   (<! (orchard.objects.wiki-pageument/wiki-page db
                             :title     document-title
                             :template  (:id tpl-doc)
                             :project   project-title))
          doc        (<! (orchard.model/load-object db (:id wiki-page)))]
      doc)))


(defn create-project
  [project-title]
  ;; TODO check for project with same name already existing
  (go
    (let [project-title  (clojure.string/replace project-title #"Project Name: " "")
          new-doc     (<! (build-document orchard.objects.app.db project-title "home"))]
      (clean-up)
      (orchard.objects.app/open-page orchard.objects.app.db (:id @new-doc)))))


(defn create-document
  [document-title]
  (go
    (let [project-title   (:current-project @orchard.objects.workspace/workspace)
          document-title  (clojure.string/replace document-title #"Document Name: " "")
          new-doc         (<! (build-document orchard.objects.app.db project-title document-title))]
      (clean-up)
      (orchard.objects.app/open-page orchard.objects.app.db (:id @new-doc)))))


(defn handle-enter
  [this text]
  (if (transient? this)
    (hide-navbar)
    (set-input-text ""))
  (condp = (:mode @this)
    :document-create
      (do
        (create-document text)
        (set-mode this :free))
    :project-create
      (do
        (create-project text)
        (set-mode this :free))))


(defn handle-esc
  [this text]
  (clean-up))


(defn handle-key
  [this text]
  (log "handle-key"))


(defn handle-keypress
  [this c]
  (go
    (loop []
      (let [ev    (<! c)
            kc    (.-keyCode ev)
            mode  (:mode @this)
            text  (get-input-text)]
        (condp = kc
          13 (handle-enter this text)
          27 (handle-esc this text)
          (handle-key this text))
        (recur)))))


(defn start-create-project
  [this]
  (when (and (transient? this) (not (visible?)))
    (show-navbar))
  (set-mode this :project-create)
  (set-input-text "Project Name: ")
  (focus))


(defn start-create-document
  [this]
  (when (and (transient? this) (not (visible?)))
    (show-navbar))
  (set-mode this :document-create)
  (set-input-text "Document Name: ")
  (focus))


(defn init-navbar
  [this]
  (aset ($nav-input) "onkeypress" orchard.objects.app/handle-keypress))


(object/behavior* ::add!
  :triggers #{:add!}
  :reaction (fn [this]
              (dom/append (dom/$ "body") (:content @this))))


(object/object* :workspace-nav
  :triggers #{:add!}
  :behaviors [::add!]
  :locked? true
  ;; track what mode the nav is in
  ;; including: project-create
  :mode :free
  ;; tracks if the nav was open or closed before begining and completing an interaction
  :transient? false
  :ready init-navbar
  :window-width (dom/window-width)
  :init (fn [this]
          (let [key-chan (chan)]
            (object/assoc! this :ready init-navbar)
            (handle-keypress this key-chan)
            [:form.navbar-form.navbar-left.main-nav
              [:div.form-group
                (home-btn)
                (new-doc-btn)
                (refresh-btn)
                (dev-tools-btn)]])))


(def workspace-nav (object/create :workspace-nav))


(dispatch/react-to #{:resize-window}
  (fn [ev & [e]]
    (object/assoc! workspace-nav :window-width
      (aget (aget e "target") "innerWidth"))))


(defn toggle
  ([]
    (toggle (not @visible*)))
  ([b]
    ;; toggle transient mode
    (object/assoc! workspace-nav :transient? (not b))
    ;; toggle the nav view
    (dom/css (dom/$ "#nav-wrapper")
      {:visibility (if (reset! visible* b) "visible" "hidden")})
    (when (visible?)
      (focus))))


(defn show-navbar [] (toggle true))
(defn hide-navbar [] (toggle false))
