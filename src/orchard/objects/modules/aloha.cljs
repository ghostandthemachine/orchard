(ns orchard.objects.modules.aloha
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [orchard.macros :refer [defui]])
  (:require [orchard.util.core  :refer [bound-do uuid]]
            [cljs.core.async    :refer [chan >! <!]]
            [orchard.module     :refer [module-view spacer default-opts
                                        edit-module-btn-icon delete-btn
                                        edit-btn handle-delete-module]]
            [orchard.util.log   :refer (log log-obj)]
            [crate.binding      :refer [bound subatom]]
            [orchard.observe    :refer [observe]]
            [orchard.object     :as object]
            [crate.core         :as crate]
            [orchard.util.dom   :as dom]
            [orchard.model      :as model]
            [orchard.util.aloha :as aloha]
            [dommy.core         :as dommy]))


(defn aloha-doc
  [db]
  (let [id (uuid)]
    (model/save-object! db id
      {:type :aloha-module
      :text ""
      :id   id})))


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
  (log "save?")
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


(defn create-module
  [app]
  (go
    (object/create :aloha-module
      (<! (aloha-doc (:db app))))))

(defn single-col
  [a]
  [:div.span12.module-content.aloha-module-content
      [:div {:class "aloha-editable"} a]])

; TODO: have to figure out how we would save multi-column content.
(defn two-col
  [a b]
  [:div.row.module-content.aloha-module-content
    [:div.span6 {:class "aloha-editable"} a]
    [:div.span6 {:class "aloha-editable"} b]])

(defn three-col
  [a b c]
  [:div.row.module-content.aloha-module-content
    [:div.span4 {:class "aloha-editable"} a]
    [:div.span4 {:class "aloha-editable"} b]
    [:div.span4 {:class "aloha-editable"} c]])


(defn render-aloha
  [content]
  (crate/html (single-col (crate/raw content))))


(def icon [:span.btn.btn-primary.aloha-icon "aloha"])


(defn init-aloha
  [this]
  (go
    (let [elem (<! (:ready-chan @this))
          ;editor (.getElementsByClassName elem "aloha-editable")
          editor (.find (js/$ elem) ".aloha-editable")]
      (aloha/aloha editor)
      (.blur editor
             (fn [event arg]
               (let [editor (.-activeEditable js/Aloha)
                     content (.getContents editor)]
                 (log "saving aloha content: " content)
                 (object/assoc! this :text content)))))))


(object/object* :aloha-module
                :tags #{:modules}
                :triggers #{:delete-module :save :ready}
                :behaviors [:orchard.module/delete-module :orchard.module/save-module]
                :label "aloha"
                :icon icon
                :text ""
                :init (fn [this record]
                        (log "new aloha module: ")
                        (log-obj (clj->js record))
                        (object/merge! this record
                          {:save-data {:last-save (get-time)
                                       :change-count 0}})
                        (bound-do (subatom this :text)
                                  (fn [& args]
                                    (object/raise this :save)))

                        ;; Watch for Aloha object DOM insertion
                        ;; init aloha if inserted
                        (init-aloha this)

                        [:div {:class (str "span12 module " (:type @this))
                               :id (str "module-" (:id @this))}
                          [:div.module-tray]
                          [:div.module-element
                            (render-aloha (:text record))]]))