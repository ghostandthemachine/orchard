(ns think.project
  (:use-macros [redlobster.macros :only [promise when-realised]]
               [dommy.macros :only [sel sel1]]
               [think.macros :only [defview]])
  (:require [think.model :as model]
            [think.log :refer [log log-obj log-err]]
            [think.util :refer [uuid ready refresh r! clipboard read-clipboard open-window editor-window]]
            [think.view-helpers :as view]
            [dommy.template :as tpl]
            [dommy.core :as dom]))

(defn create
  [elem-type]
  (.createElement js/document (str (name elem-type))))

(def pages
  #{{:id 1
     :content "wlngwkrjgnw;kjgnw;gn ngkjwngwk;jgnw"}
    {:id 2
     :content "nuyltiuyl,fdtjfhx"}
    {:id 3
     :content "eytkeutketnyytejtewlngwkrjgnw;kjgnw;gn ngkjwngwk;jgnw"}
    {:id 4
     :content "rjw5jwrtjwryjwrjy;kjgnw;gn ngkjwngwk;jgnw"}
    {:id 5
     :content "msfmnrsyjrjt;kjgnw;gn ngkjwngwk;jgnw"}})

(def test-projects
  {1 {:title "Something Interesting"
      :meta
       {:author "Joe Smith"
        :team "dev"}
     :pages pages}
   2 {:title "Foooooo Baaaarrrr"
      :meta
       {:author "Joe Smith"
        :team "dev"}
        :pages pages}
   3 {:title "Another interesting project"
      :meta
       {:author "Joe Smith"
        :team "dev"}
        :pages pages}})

(defview new-project-form
  []
  [:form
   [:input#new-project-input {:type "text" :name "project-name"}]
   [:input#new-project-btn {:type "submit" :value "+"}]]
  :submit #(log (str "New Project: " (.-value (sel1 :#new-project-input)))))

(defview project-list-item
  [p]
  [:li (:title p)]
  :click #(log (str "Select project " (:title p))))

(defview project-menu
  [projects]
  [:div
   [:h3 "Projects"]
   (new-project-form)
   [:ul (map project-list-item (vals test-projects))]])

(defn init
  []
  (dom/append! (sel1 :body) (project-menu (vals test-projects))))

;  (on-realised (model/all-projects)
;               #(.appendChild (sel1 :body)
;                              (project-menu %))))
;
