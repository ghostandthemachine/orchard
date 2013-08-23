(ns think.objects.modules.tiny-mce
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [think.macros :refer [defui]])
  (:require [think.object    :as object]
            [crate.core      :as crate]
            [think.util.core :refer [bound-do uuid]]
            [think.util.dom  :as dom]
            [cljs.core.async :refer [chan >! <!]]
            [think.module    :refer [module-view spacer default-opts
                                  edit-module-btn-icon delete-btn edit-btn]]
            [think.util.log  :refer (log log-obj)]
            [crate.binding   :refer [bound subatom]]
            [think.model     :as model]
            [think.observe   :refer [observe]]
            [dommy.core      :as dommy]))


(defn tiny-mce-doc
  []
  (model/save-document
    {:type :tiny-mce-module
     :text ""
     :id   (uuid)}))


(def date (new js/Date))


(defn get-time
  []
  (.getTime date))


(defn hiccup->str
  [form]
  (.-outerHTML
    (crate.core/html
      form)))


(defn render-think-link
  [tag link]
  (str " "
    (hiccup->str
      [:span.think-link {:data-href link
                         :data-title tag
                         :style {:background-color  "rgb(241, 241, 241)"
                                 :padding-bottom    "3px"
                                 :padding-top       "2px"
                                 :padding-left      "5px"
                                 :padding-right     "5px"
                                 :color             "green"
                                 :border-radius     "4px"}} tag])
    " "))



(defn open-from-link
  [href]
  (go
    (let [[project-title title] (clojure.string/split href #"/")
          all-docs     (<! (model/all-wiki-documents))
          projects     (reduce
                        (fn [m wiki-doc]
                          (let [proj (or (:project wiki-doc) "No Project")]
                            (assoc-in m [(clojure.string/lower-case proj) (clojure.string/lower-case (:title wiki-doc))]
                              wiki-doc)))
                        {}
                        all-docs)]
      (if-let [d (get-in projects [(clojure.string/lower-case project-title) (clojure.string/lower-case title)])]
        (think.objects.app/open-document (:_id d))
        (log "Tried to open document that doesn't exist")))))


(defn handle-tiny-click
  [ed e]
  (let [el (.-target e)]
    (when (= "think-link" (.-className el))
      (let [href  (.getAttribute el "data-href")
            title (.getAttribute el "data-title")]
        (log "open think-link: " href)
        (open-from-link href)))))


(defn replace-think-links
  [s]
  (let [regex #"\[([^\]]+)\]\(([^)]+)\)"]
    (clojure.string/replace s
      regex
      (fn [m]
        (let [[res tag link] (re-find regex m)]
          (render-think-link tag link))))))


(def MAX-SAVE-DIFF  10000)
(def MAX-CHANGE-DIFF 5)


(defn save?
  [this]
  (let [time-diff  (- (get-in @this [:save-data :last-save]) (get-time))
        press-diff (get-in
                      (object/update! this [:save-data :change-count] inc)
                      [:save-data :change-count])
        s?         (or
                     (> time-diff MAX-SAVE-DIFF)
                     (> press-diff MAX-CHANGE-DIFF))]
    (if s?
      (do
        (object/assoc! this :save-data {:last-save (get-time)
                                        :change-count 0})
        true)
      false)))


(defn render-editor
  [this]
  (let [el [:div.module-content.tiny-mce-module-content
             [:form {:method "post"}
              [:textarea {:class (str "tiny-mce-editor-" (:id @this))}]]]
        html (crate.core/html el)]
    html))


(def icon [:span.btn.btn-primary.tiny-mce-icon "TinyMCE"])


(defn handle-editor-change
  [this ed l]
  (object/assoc! this :text (.getContent ed)))


(defn load-text
  [this ed]
  (.setContent ed (replace-think-links (:text @this))))


(defn handle-editor-mutations
  [this mutations]
  (go
    (>! (:observer-chan @this) mutations)))


(defn observe-mutations
  [this]
  (go
    (let [mutations (<! (:observer-chan @this))]
      (log-obj mutations))))


(defn handle-editor-init
  [this ed]
  (let [body (first (.select (.-dom ed) "body"))]
    ;; finally load text from record
    (load-text this ed)))


(defn handle-set-content
  [ed o]
  (let [content     (.getContent ed)
        new-content (replace-think-links content)]
    (aset o "content" new-content)))


(defn setup-tinymce
  [this ed]
  (let [on-change       (.-onChange ed)
        on-init         (.-onInit ed)
        on-set-content  (.-onSetContent ed)
        on-click        (.-onClick ed)]
    (.add on-init   (partial handle-editor-init this))
    (.add on-change (partial handle-editor-change this))
    (.add on-set-content handle-set-content this)
    (.add on-click handle-tiny-click)))


(defn init-tinymce
  [this]
  (.init js/tinyMCE
    (clj->js
      {:theme                 "advanced"
       :mode                  "specific_textareas"
       :editor_selector       (str "tiny-mce-editor-" (:id @this))
       :theme_advanced_buttons1 "mybutton, bold, italic, underline, strikethrough, separator,
                                 link, unlink, image, code, hr, separator,
                                 styleselect, formatselect, fontselect, fontsizeselect, seperator,
                                 forecolorpicker, backcolorpicker, separator,
                                 justifyleft, justifycenter, justifyright, justifyfull, separator,
                                 bullist, numlist, undo, redo"
       :theme_advanced_buttons2 ""
       :theme_advanced_buttons3 ""
       :theme_advanced_toolbar_location "top"
       :theme_advanced_toolbar_align "left"
       :theme_advanced_statusbar_location "bottom"
       :plugins               "autoresize,inlinepopups"
       :width                 "100%"
       :setup                 (partial setup-tinymce this)})))


(object/object* :tiny-mce-module
                :tags #{:modules}
                :triggers #{:delete-module :save :ready}
                :behaviors [:think.module/delete-module :think.module/save-module ::ready]
                :label "TinyMCE"
                :icon icon
                :text ""
                :editor nil
                :observer-chan nil
                :save-data {:last-save    nil
                            :change-count  nil}
                :init (fn [this record]
                        (object/merge! this record
                          {:ready (partial init-tinymce this)
                           :save-data {:last-save (get-time)
                                       :change-count 0}
                           :observer-chan (chan)})
                        (bound-do (subatom this :text)
                                  (fn [& args]
                                    (log "inside :text handler...")
                                    (object/raise this :save)))
                        
                        [:div {:class (str "span12 module " (:type @this))
                               :id (str "module-" (:id @this))}
                          [:div.module-tray]
                          [:div.module-element
                            (render-editor this)]]))


(defn create-module
  []
  (go
    (object/create :tiny-mce-module
      (<! (tiny-mce-doc)))))
