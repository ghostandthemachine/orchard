(ns think.objects.modules.tiny-mce
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [think.macros :refer [defui]])
  (:require [think.object    :as object]
            [crate.core      :as crate]
            [think.util.core :refer [bound-do uuid]]
            [think.util.dom  :as dom]
            [think.module    :refer [module-view spacer default-opts
                                  edit-module-btn-icon delete-btn edit-btn]]
            [think.util.log  :refer (log log-obj)]
            [crate.binding   :refer [bound subatom]]
            [think.model     :as model]
            [dommy.core      :as dommy]))

(def date (new js/Date))


(defn get-time
  []
  (.getTime date))


(def MAX-SAVE-DIFF  10000)
(def MAX-PRESS-DIFF 20)


(defn save?
  [this]
  (let [time-diff  (- (get-in @this [:save-data :last-save]) (get-time))
        press-diff (get-in
                      (object/update! this [:save-data :press-count] inc)
                      [:save-data :press-count])
        s?         (or
                     (> time-diff MAX-SAVE-DIFF)
                     (> press-diff MAX-PRESS-DIFF))]
    (if s?
      (do
        (object/assoc! this :save-data {:last-save (get-time)
                                        :press-count 0})
        true)
      false)))


(defn tiny-mce-doc
  []
  (model/save-document
    {:type :tiny-mce-module
     :text ""
     :id   (uuid)}))


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
  (when (save? this)
    (log "Attempt to save tiny-mce editor")
    (log-obj (.getContent ed))
    (object/assoc! this :text (.getContent ed))))


(defn load-text
  [this ed]
  (log "load editor text from record")
  (.setContent ed (:text @this)))


(defn setup-tinymce
  [this ed]
  (let [on-change (.-onKeyPress ed)
        on-init   (.-onInit ed)]
    (.add on-init (partial load-text this ed))
    (.add on-change (partial handle-editor-change this))))


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
                :save-data {:last-save    nil
                            :press-count  nil}
                :init (fn [this record]
                        (object/merge! this record
                          {:ready (partial init-tinymce this)
                           :save-data {:last-save (get-time)
                                       :press-count 0}})
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
    (let [doc (<! (tiny-mce-doc))
          obj (object/create :tiny-mce-module doc)]
      obj)))


; (dommy/listen! [(dom/$ :body) :.tiny-mce-module-content :a] :click
;   (fn [e]
;     ; (log "loading document: " (keyword (last (clojure.string/split (.-href (.-target e)) #"/"))))
;     (think.objects.app/open-document
;       (last (clojure.string/split (.-href (.-target e)) #"/")))
;     (.preventDefault e)))