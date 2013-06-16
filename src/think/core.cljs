(ns think.core
  (:use-macros [dommy.macros :only [sel sel1 node]])
  (:require [dommy.core :as dom] 
            [think.util.core :refer [log log-obj]]))


(def STYLESHEETS
  ["css/codemirror.css",
   "bootstrap/css/bootstrap.min.css",
   "font-awesome/css/font-awesome.min.css",
   "css/jquery-ui.css",
   "css/thinker.css"])


(def SCRIPTS
  ["js/markdown_parser.js",
   "js/d3.v3.min.js",
   "js/codemirror.js",
   "js/cm/clojure.js",
   "js/cm/markdown.js",
   "js/cm/gfm.js",
   "js/jquery-1.9.0.js",
   "js/jquery.sortable.min.js",
   "js/jquery-ui.js",
   "js/throttle.js",
   ;"js/pdf/compatibility.js",
   ;"js/pdf/l10n.js",
   "js/pdf.js",
   ;"js/pdf/debugger.js",
   ;"js/pdf/viewer.js",
   "bootstrap/js/bootstrap.min.js",
   "js/thinker.js"])


(def $body (sel1 :body))
(def $head (sel1 :head))

(defn load-script
  [path]
  (let [fpath  (str "file://" path)]
    (dom/append! $body
                 [:script {:type "text/javascript"
                           :async false
                           :src fpath}])))


(defn load-css
  [path]
  (let [fpath  (str "file://" path)]
    (dom/append! $head
                 [:link {:type "text/css"
                         :rel "stylesheet"
                         :href fpath}])))



(defn load-stylesheets
  []
  (doall (map load-css STYLESHEETS)))


(defn load-javascripts
  []
  (doall (map load-script SCRIPTS)))


(defn start-app
  []
  (try
    (load-stylesheets)
    (load-javascripts)
    ;(js/process.on handle-errors)
    ;(aset window "onerror" handle-errors)
    (catch js/Object e
      (log (str "Loading Error: " e))
      (log-obj e))))

;(start-app)
