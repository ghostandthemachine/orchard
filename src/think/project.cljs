(ns think.project
  (:use-macros [redlobster.macros :only [promise when-realised]]
               [dommy.macros :only [sel sel1]]
               [think.macros :only [defview]])
  (:require [think.db :as db]
            [think.log :refer [log log-obj log-err]]
            [think.util :refer [uuid ready log log-obj refresh r! clipboard read-clipboard open-window editor-window]]
            [think.view-helpers :as view]
            [dommy.template :as dt]
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

(def projects
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







(defn handler
  [e]
  (log "handling " e))

(defview nav-element
  [m]
  [:li ]
  :click handler)



(defn init
  []
  )



(defn watch-node
  [prop handler]
  )

(defn watch
  []
  )

; (dispatch/react-to #{:edge-selected} #(log "edge selected "))
; (dispatch/fire :edge-removed [edge-id edge-map])

; (defn watch-property-handler
;   [property handler]
;   (this-is this
;     (let [oldval (aset this "oldval" (aget this property))
;           thisewval (aset this "newval" oldval)
;           getter (fn [] (aget this "newval"))
;           setter (fn [val]
;                     (log "updateing " val)
;                     (aset this "oldval" (aget this "newval"))
;                     (aset this "newval" (handler this property (aget this "oldval") val)))])))

; (def watch-prototype
;   {:enumerable   false
;    :configurable true
;    :writable     false
;    :value nil})

; (defn make-watchable
;   []
;   (set! (.. js/Object -prototype -watch) watch-property-handler))