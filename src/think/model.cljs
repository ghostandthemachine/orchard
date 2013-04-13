(ns think.model
  (:refer-clojure  :exclude [create-node])
  (:use [think.log :only    (log log-obj)])
  (:use-macros [redlobster.macros :only [let-realised]])
  (:require [think.util         :as util]
            [think.dispatch     :refer [fire react-to]]
            [think.db           :as db]
            [redlobster.promise :as p]))


(defn project
  [title]
  {:type ::project
   :id      (util/uuid)
   :title   title
   :updated (util/date-json)})


(defn wiki-page
  [project]
  {:type ::wiki-page
   :id      (util/uuid)
   :project project
   :modules []
   :updated (util/date-json)
   })


(defn wiki-module
  []
  {:type ::wiki-module
   :id    (util/uuid)
   :title "title"
   :body  "body"
   })


(defn document
  []
  {:type ::document
   :id          (util/uuid)
   :title       "title"
   :authors     []
   :path        "path"
   :filename    "filename.pdf"
   :notes       []
   :annotations []
   :cites       []
   :tags        []
   })


(def DB-PATH "data/project.db")

(def project-db* (atom nil))

(defn init-project-db
  []
  (when (nil? @project-db*)
    (let [db-promise (db/open DB-PATH)]
      (p/on-realised db-promise
        #(do
           (reset! project-db* %)
           (fire :db-ready @project-db*))
        (fn [err]
          (log "Error opening DB!")
          (log-obj err))))))


(defn create-project
  [title]
  (let [proj (project title)]
    (db/create-doc @project-db* (clj->js proj))))


(defn delete-project
  [proj]
  (db/delete-doc @project-db* (clj->js proj)))


(defn await
  "Replaces redlobster await. The original version took & promises which didn't support a seq of promises.
  This version expects a seq. In addition, the original one simply returned :redlobster.promise/realised,
  this version returns a map of the original promises which await wrapped. Since all promises have been
  realised by the time this function returns, it also maps deref and js->clj on the promise resulting in a
  seq of resolved objects which were the initial promise targets. Bam!"
  [promises]
  (let [await-all (= (first promises) :all)
        promises (if await-all (rest promises) promises)
        p (p/promise)
        total (count promises)
        count (atom 0)
        done (atom false)]
    (doseq [subp promises]
      (let [succ (fn [_]
                   (when (not @done)
                     (swap! count inc)
                     (when (= total @count)
                       (reset! done true)
                       (p/realise p (map #(js->clj (deref %)) promises)))))
            fail (if await-all succ
                     (fn [err]
                       (when (not @done)
                         (reset! done true)
                         (p/realise-error p err))))]
        (p/on-realised subp succ fail)))
    p))



(defn all-projects
  []
  (let-realised [docs (db/all-docs @project-db*)]
    (await (map #(db/get-doc @project-db* (.-id %)) (.-rows @docs)))))

