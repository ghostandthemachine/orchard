(ns orchard.objects.modules.tiny-mce
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [orchard.macros :refer [defui]])
  (:require [orchard.object    :as object]
            [crate.core      :as crate]
            [orchard.util.core :refer [bound-do uuid]]
            [orchard.util.dom  :as dom]
            [cljs.core.async :refer [chan >! <!]]
            [orchard.module    :refer [module-view spacer default-opts
                                  edit-module-btn-icon delete-btn edit-btn handle-delete-module]]
            [orchard.util.log  :refer (log log-obj)]
            [crate.binding   :refer [bound subatom]]
            [orchard.model     :as model]
            [orchard.observe   :refer [observe]]
            [dommy.core      :as dommy]))


(defn tiny-mce-doc
  [db]
  (model/save-object db
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


(defn render-orchard-link
  [tag link]
  (str " "
    (hiccup->str
      [:span.orchard-link {:data-href link
                         :data-title tag
                         :style {:background-color  "rgb(241, 241, 241)"
                                 :padding-bottom    "3px"
                                 :padding-top       "2px"
                                 :padding-left      "5px"
                                 :padding-right     "5px"
                                 :color             "green"
                                 :border-radius     "4px"}} tag])
    " "))


(defn handle-tiny-click
  [ed e]
  (let [el (.-target e)]
    (when (= "orchard-link" (.-className el))
      (let [href  (.getAttribute el "data-href")
            title (.getAttribute el "data-title")]
        (log "open orchard-link: " href)
        (orchard.objects.app/open-from-link href)))))


(def link-regex #"\[([^\]]+)\]\(([^)]+)\)")


(defn replace-orchard-links
  [s]
  (clojure.string/replace s
    link-regex
    (fn [m]
      (let [[res tag link] (re-find link-regex m)]
        (render-orchard-link tag link)))))


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


(def editor-ids* (atom []))


(defn get-editors [] @editor-ids*)


(defn add-editor-id!
  [ed]
  (swap! editor-ids* conj ed))


(defn remove-editor-id!
  [ed-id]
  (swap! editor-ids*
    (fn [eds]
      (filter #(not= ed-id %) eds))))


(defn format-class
  [id]
  (str "tiny-mce-editor-" id))


(defn get-editor
  [ed-id]
  (.get js/tinyMCE (format-class ed-id)))


(defn clear-editors!
  []
  (reset! editor-ids* []))


(defn create-module
  [app]
  (go
    (object/create :tiny-mce-module
      (<! (tiny-mce-doc (:db app))))))



(defn render-editor
  [this]
  (let [el [:div.module-content.tiny-mce-module-content
             [:form {:method "post"}
              [:textarea {:id (str "tiny-mce-editor-" (:id @this))}]]]
        html (crate.core/html el)]
    (add-editor-id! (:id @this))
    html))


(def icon [:span.btn.btn-primary.tiny-mce-icon "TinyMCE"])


(defn handle-editor-change
  [this ed l]
  (log "handle editor change")
  (object/assoc! this :text
    (replace-orchard-links (.getContent ed))))


(defn handle-node-change
  [ed cm e]
  ; (when (re-find link-regex (aget e "outerHTML"))
  ;   (log-obj e)
  ;   (.setContent ed (replace-orchard-links (.getContent ed))))
  )

(defn handle-editor-mutations
  [this mutations]
  (go
    (>! (:observer-chan @this) mutations)))


(defn load-text
  [this ed]
  (.setContent ed (replace-orchard-links (:text @this))))


(defn handle-editor-init
  [this ed]
  (let [body (first (.select (.-dom ed) "body"))]
    ;; finally load text from record
    (load-text this ed)))


(defn handle-set-content
  [ed o]
  (let [content     (.getContent ed)
        new-content (replace-orchard-links content)]
    (aset o "content" new-content)))


(defn setup-tinymce
  [this ed]
  (let [on-change       (.-onChange ed)
        on-init         (.-onInit ed)
        on-set-content  (.-onSetContent ed)
        on-click        (.-onClick ed)
        on-node-change  (.-onNodeChange ed)]
    (.add on-init   (partial handle-editor-init this))
    (.add on-change (partial handle-editor-change this))
    (.add on-set-content handle-set-content this)
    (.add on-click handle-tiny-click)
    (.add on-node-change handle-node-change)

    (.addButton ed
      "deletemodule"
      (clj->js
        {:title "Delete Module"
         :image "images/trash-can.png"
         :onclick (partial handle-delete-module this)}))))


(defn init-tinymce
  [this]
  (.init js/tinyMCE
    (clj->js
      {:theme                 "advanced"
       :mode                  "exact"
       :elements              (format-class (:id @this))
       :theme_advanced_buttons1 "mybutton, bold, italic, underline, strikethrough, separator,
                                 link, unlink, image, code, hr, separator,
                                 formatselect, fontselect, fontsizeselect, seperator,
                                 forecolorpicker, backcolorpicker, separator,
                                 justifyleft, justifycenter, justifyright, justifyfull, separator,
                                 bullist, numlist, separator,
                                 deletemodule"
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
                :behaviors [:orchard.module/delete-module :orchard.module/save-module ::ready]
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
