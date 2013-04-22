(ns think.project
  (:use-macros [redlobster.macros :only [when-realised let-realised]]
               [dommy.macros :only [sel sel1]]
               [think.macros :only [defui]])
  (:require [redlobster.promise :refer [promise on-realised]]
            [think.dispatch :refer [fire react-to]]
            [think.model :as model]
            [think.util.log :refer [log log-obj log-err]]
            [think.util :refer [uuid ready refresh r! clipboard read-clipboard open-window editor-window]]
            [think.view-helpers :as view]
            [dommy.template :as tpl]
            [dommy.core :as dom]
            [redlobster.promise :as p]))

(defn create
  [elem-type]
  (.createElement js/document (str (name elem-type))))

(def cur-project* (atom nil))

(defui new-project-form
  []
  [:div
    [:form
     [:input#new-project-input {:type "text" :name "project-name"}]
     [:input#new-project-btn {:type "submit" :value "+"}]]]
  :submit #(model/create-project (.-value (sel1 :#new-project-input))))


(defui project-list-item
  [p]
  [:li (.-title p)]
  :click (fn [e]
            (reset! cur-project* (.-id p))
            (fire :load-project (.-content p))))


(defui project-menu
  [projects]
  [:div#project-nav
   [:h3 "Projects"]
   (new-project-form)
   [:ul (map project-list-item projects)]])


(defn init
  []
  (model/init-project-db)
  (react-to #{:db-ready}
    (fn [_ _]
      (let [all-projects-promise (model/all-projects)]
        ; (when-realised [all-projects-promise]
        ;    (dom/prepend! (sel1 :body)
        ;                  (project-menu @all-projects-promise)))
        ))))

(react-to #{:save-editor-text}
  (fn [ev & [data]]
    (model/update-project
      {:_id @cur-project*
       :content data})))