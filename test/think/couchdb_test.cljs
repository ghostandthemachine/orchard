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
      (is= ["_replicator" "_users" "projects"] (<! (db/list-all)))))

  ; (deftest create-delete-db-test
  ;   (let [db-name "create-db-test-db"]
  ;     (testing "should create a new db"
  ;       (go
  ;         (<! (db/create db/name db-name))
  ;         (is
  ;           (not (nil? (some #{db-name} (<! (db/list-all))))))))

  ;     (testing "should delete a db"
  ;       (go
  ;         (is 
  ;           (nil? (some #{db-name} (<! (db/list-all)))))))))


  ; (deftest create-delete-doc
  ;   (let [doc-id   "create-delete-doc"
  ;         test-doc {:id   doc-id
  ;                   :foo  "bar"}]
  ;     (db/update-doc nano test-doc)
  ;     (testing "should create a new document record"
  ;       (go
  ;         (is= {} (<! (db/get-doc test-db* doc-id)))))

  ;     ; (testing "should delete a new document record"
  ;     ;   (go
  ;     ;     (<! (db/delete-doc test-db* doc-id))
  ;     ;     (is
  ;     ;       (not ))))
  ;     ))
)

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
  (log "Performance Test")
  (go
    (let [num-docs    100
          init-time   (now) 
          _           (<! (create-test-docs db num-docs))
          create-time (now) 
          _           (<! (read-test-docs db num-docs))
          read-time   (now)]
      (log "Create time: " (/ (- create-time init-time) num-docs))
      (log "Read time: " (/ (- read-time create-time) num-docs)))))

(defn run-tests
  []
  (go
    (let [db (<! (db/open "test-db"))]
      (log "New test db")
      (log-obj db)
      (all-tests db)


      ; (<! (perf-test db))

      (<! (db/delete-db "test-db")))))





  