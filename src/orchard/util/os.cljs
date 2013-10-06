(ns orchard.util.os
  (:require-macros [orchard.macros :refer [defonce]])
  (:require [clojure.browser.repl :as repl]
            [orchard.util.log :refer (log log-obj)]))

(def ^:private fs            (js/node_require "fs"))
(def ^:private child-process (js/node_require "child_process"))

(def shell (.-exec child-process))
(def spawn (.-spawn child-process))

(def DEFAULT-KILL-TIMEOUT 1000)

(defonce child-processes* (atom {}))


(defn process
  "Spawn a child process.  Will be run as a shell command if :shell true is passed."
  [cmd & {:keys [shell]}]
  (let [child (spawn cmd)]
    (swap! child-processes* assoc (.-pid child) child)
    child))


(defn kill-children
  ([] (kill-children DEFAULT-KILL-TIMEOUT))
  ([timeout] (kill-children timeout "SIGQUIT"))
  ([timeout sig]
   (doseq [[pid child] @child-processes*]
     (log "killing child process: " pid)
     (.kill child sig))))


(defn env
  "Returns the value of the environment variable k,
   or raises if k is missing from the environment."
  [k]
  (let [e (js->clj (.env node/process))]
    (or (get e k) (throw (str "missing key " k)))))


(defn trap
  "Trap the Unix signal sig with the given function."
  [sig f]
  (.on node/process (str "SIG" sig) f))


(defn exit
  "Exit with the given status."
  [status]
  (.exit node/process status))


;; Opening files and folders externally

(defn open-file
  "Open a file with the OS default app given its mime-type."
  [path]
  (js/gui.Shell.openItem path))


(defn show-file
  "Show a file in the finder."
  [path]
  (js/gui.Shell.showItemInFolder path))


(defn write-file
  "Write a string to a text file."
  [path string]
  (.writeFile fs path string))


;; External Browser

(defn open-url
  "Open a URL in the default web browser."
  [url]
  (js/gui.Shell.openExternal url))

