(ns think.objects.animations
  (:require [think.object :as object]
            [think.util.dom :as dom]))

(def $body (dom/$ :body))

(defn on []
  (dom/add-class $body :animated))

(defn off []
  (dom/remove-class $body :animated))

(defn on? []
  (dom/has-class? $body :animated))

(object/behavior* ::animate-on-init
                  :triggers #{:init}
                  :reaction (fn [app]
                              (on)))

(object/tag-behaviors :app [::animate-on-init])