(ns orchard.objects.modules.aloha
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
            [orchard.util.aloha :as aloha]
            [dommy.core      :as dommy]))


(defn aloha-doc
  []
  (model/save-document
    {:type :aloha-module
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


(defn create-module
  []
  (go
    (object/create :aloha-module
      (<! (aloha-doc)))))

(defn single-col
  [this]
  [:div.row.module-content.aloha-module-content
      [:div.span12 {:class (str "aloha-" (:id @this))}]])

(defn two-col
  [this]
  [:div.row.module-content.aloha-module-content
    [:div.span6 {:class (str "aloha-" (:id @this))}]
    [:div.span6 {:class (str "aloha-" (:id @this))}]])

(defn three-col
  [this]
  [:div.row.module-content.aloha-module-content
    [:div.span4 {:class (str "aloha-" (:id @this))}]
    [:div.span4 {:class (str "aloha-" (:id @this))}]
    [:div.span4 {:class (str "aloha-" (:id @this))}]])


(defn render-aloha
  [this]
  (crate.core/html (single-col this)))


(def icon [:span.btn.btn-primary.aloha-icon "aloha"])


(defn setup-aloha
  [this ed]
  )


(defn init-aloha
  [this]
  ; (let [sel (str "aloha-" (:id @this))]
  ;   (aloha/$aloha sel))
  )


(object/object* :aloha-module
                :tags #{:modules}
                :triggers #{:delete-module :save :ready}
                :behaviors [:orchard.module/delete-module :orchard.module/save-module]
                :label "aloha"
                :icon icon
                :text ""
                :init (fn [this record]
                        (object/merge! this record
                          {:save-data {:last-save (get-time)
                                       :change-count 0}})
                        (bound-do (subatom this :text)
                                  (fn [& args]
                                    ; (log "inside :text handler...")
                                    (object/raise this :save)))
                        
                        [:div {:class (str "span12 module " (:type @this))
                               :id (str "module-" (:id @this))}
                          [:div.module-tray]
                          [:div.module-element
                            (render-aloha this)]]))
