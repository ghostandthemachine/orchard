(ns orchard.git
  (:refer-clojure :exclude [create-node])
  (:require-macros
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require
    [cljs.core.async    :refer (chan >! <! timeout put! take!)]
    [orchard.util.log     :refer (log log-obj)]
    [orchard.util.core    :as util]
    [orchard.util.time    :as time]
    [orchard.util.fs      :as fs]
    [orchard.util.nw      :as nw]))


(def ^:private git (js/require "nodegit"))

(def PROJECT-DIR (fs/join (nw/data-path) "projects"))

(if (not (fs/exists? PROJECT-DIR))
  (fs/mkdir PROJECT-DIR))

(defn oid
  [sha-str]
  (.Oid.fromString git sha-str))

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


(defn project-repo
  [proj-id]
  (let [proj-dir (fs/join PROJECT-DIR proj-id)]
    (go
      (<!
        (if (<! (fs/exists? proj-dir))
          (do
            (log "found repo: " proj-dir)
            (repo proj-dir))
        (do
          (log "making new project repo: " proj-dir)
          (fs/mkdir proj-dir)
          (create-repo proj-dir)))))))


(defn with-branch*
  [repo branch-name]
  (let [branch-chan (chan)]
    (.branch repo branch-name
             (fn [error, branch]
               (put! branch-chan (if error nil branch))))
    branch-chan))


(defn revision-history-chan
  [branch]
  (let [rev-chan (chan)]
    (.tree branch
           (fn [error tree]
             (.on (.walk tree) "entry"
                  (fn [error entry] (put! rev-chan entry)))))
    rev-chan))


(defn fetch
  "Fetch from a remote repo, downloading new objects into the local repo."
  [repo remote-name]
  (let [remote (.getRemote repo remote-name)]
    (.connect remote 0
              (fn [err]
                (if err
                  (log "Error connecting to remote: " remote-name)
                  (.download remote (fn [dl-err]
                                      (if dl-err
                                        (log "Error downloading from remote: " dl-err)))))))))


(defn get-commit
  [repo sha]
  (let [com-chan (chan)]
    (.getCommit repo sha
                (fn [err, commit] (put! com-chan commit)))))

