(ns orchard.core
  (:require [orchard.objects.app :as app]))

(defn init []
  (app/init))