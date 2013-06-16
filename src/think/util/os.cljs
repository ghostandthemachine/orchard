(ns think.util.os
  (:use [think.util.log :only (log log-obj)])
  (:require [redlobster.promise :as p]
            [clojure.browser.repl :as repl]))

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


