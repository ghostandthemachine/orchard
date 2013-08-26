(ns orchard.git
  (:refer-clojure :exclude [create-node])
  (:require-macros
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require
    [cljs.core.async    :refer (chan >! <! timeout)]
    [orchard.util.log     :refer (log log-obj)]
    [orchard.util.core    :as util]
    [orchard.util.time    :as time]
    [orchard.util.fs      :as fs]
    [orchard.util.nw      :as nw]))


(def ^:private git (js/require "nodegit"))

(def PROJECT-REPO "projects")


(defn create-repo
  [path]
  (let [res-chan (chan)
        ctor (.-repo git)
        repo (new ctor)]
    (log "creating new git repo: " path)
    (.init repo path true
           (fn [err repo]
             (go
               (if err
                 (log "Error initializing git repo:")
                 (log-obj err)
                 (>! res-chan repo)))))
    res-chan))


(defn repo
  [path]
  (let [res-chan (chan)
        ctor     (.-repo git)]
    (log "opening git repo: " path)
    (new ctor path
               (fn [err repo]
                 (go
                   (if err
                     (do
                       (log "Error opening git repo:")
                       (log-obj err))
                     (>! res-chan repo)))))
    res-chan))


(defn app-repo
  []
  (let [app-dir (fs/join (nw/data-path) PROJECT-REPO)
        config  (fs/join app-dir "config")]
    (go
      (<!
        (if (<! (fs/exists? config))
        (repo app-dir)
        (do
          (fs/mkdir app-dir)
          (create-repo app-dir)))))))



