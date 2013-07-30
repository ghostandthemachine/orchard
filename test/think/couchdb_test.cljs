(ns test.think.couchdb-test
  (:refer-clojure  :exclude [create-node])
  (:require
    [think.util.core    :as util]
    [think.util.time    :refer (now)]
    [think.couchdb      :as db]
    [think.object       :as object]
    [think.util.log     :refer (log log-obj)]
    [cljs.core.async    :refer (chan >! <! close!)])
  (:require-macros
    [test.helpers :refer [is= is deftest testing runner]]
    [cljs.core.async.macros :refer (go)]))


(defn all-tests
  [db]
      
  (deftest couch-ids-test
    (testing "should convert both id and rev keys to _id and _rev"
      (is= (db/couch-ids {:id "test-id" :rev "rev-id"}) {"_id" "test-id" "_rev" "rev-id"})))


  (deftest cljs-ids-test
    (testing "should convert both \"_id\" and \"_rev\" keys to :id and :rev"
      (is= (select-keys (db/cljs-ids {:_id "test-id" :_rev "rev-id"})
                        [:id :rev])
           {:id "test-id" :rev "rev-id"})))


  (deftest list-all-test
    (go
      (is= ["_replicator" "_users" "projects" "test-db"] (<! (db/list-all)))))


  ; (deftest create-delete-db-test
  ;   (go
  ;     (let [db-name "create-db-test-db"
  ;           db        (<! (db/create db/name db-name))
  ;           db-list   (<! (db/list-all))
  ;           res       (<! (db/delete-db db-name))
  ;           del-list  (<! (db/list-all))]
  ;       (testing "should create a new db"
  ;         (is
  ;           (not (nil? (some #{db-name} db-list)))))

  ;       (testing "should delete a db"
  ;         (is 
  ;           (nil? (some #{db-name} (<! (db/list-all)))))))))


  (deftest create-delete-doc
    (go
      (let [doc-id   "create-delete-doc"
            test-doc {:id    doc-id
                      :type  "test-doc"}
            new-doc   (<! (db/update-doc db test-doc))
            res-doc   (<! (db/get-doc db doc-id))
            del       (<! (db/delete-doc db res-doc))
            del-doc   (<! (db/get-doc db doc-id))]
        (testing "should create a new document record"
          (is= test-doc (dissoc res-doc :rev)))

        (testing "should delete a new document record"
          (is (nil? del-doc)))))))

(defn create-test-docs
  [db n]
  (go
    ; (log "Creating test docs")
    (loop [i 0]
      ; (log "new doc: " i)
      (when (< i n)
        (<!
          (db/update-doc db
            {:id   (str i)
             :type :test-doc}))
        (recur (inc i))))))


(defn read-test-docs
  [db n]
  (go
    ; (log "Reading test docs")
    (loop [i 0]
      ; (log "reading doc: " i)
      (when (< i n)
        (<!
          (db/get-doc db i))
        (recur (inc i))))))


(defn perf-test
  [db]
  (go
    (let [num-docs    5
          init-time   (now) 
          _           (<! (create-test-docs db num-docs))
          create-time (now) 
          _           (<! (read-test-docs db num-docs))
          read-time   (now)
          create-time (/ (- create-time init-time) num-docs)
          read-time   (/ (- (now) create-time) num-docs)
          total-time  (+ read-time create-time)]
      (log "")
      (log "Created and read " num-docs " documents in " (/ total-time 1000) "s")
      (log "")
      (log "Create time: " (/ create-time 1000) "s")
      (log "Read time: " (/ read-time 1000) "s")
      (log ""))))

(defn run-tests
  []
  (go
    (let [db (<! (db/open "test-db"))]
      (log "Created test db")
      (log "")

      (log "Running tests...")
      (all-tests db)
      (log "Finished running tests")
      (log "")

      (log "Running performance tests...")
      (<! (perf-test db))
      (log "Finished running performance tests")
      (log "")

      (<! (db/delete-db "test-db"))
      (log "Deleted test db")
      (log "")
      
      (log "Finished testing"))))





  