(ns think.project
  (:use-macros [redlobster.macros :only [when-realised let-realised]]
               [dommy.macros :only [sel sel1]]
               [think.macros :only [defview]])
  (:require [redlobster.promise :refer [promise on-realised]]
            [think.dispatch :refer [fire react-to]]
            [think.model :as model]
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
  :submit #(model/create-project (.-value (sel1 :#new-project-input))))


(defview project-list-item
  [p]
  [:li (:title p)]
  :click #(log (str "Select project " (:title p))))


(defview project-menu
  [projects]
  [:div
   [:h3 "Projects"]
   (new-project-form)
   [:ul (map project-list-item projects)]])


(defn init
  []
  (model/init-project-db)
  (comment react-to #{:db-ready}
            (fn [_ _]
              (let-realised [projects (model/all-projects)]
                (let [projects (js->clj (.-rows @projects) :keywordize-keys true)]
                   (log-obj projects)
                   (dom/append! (sel1 :body)
                                 (project-menu (map :doc projects))))))))


