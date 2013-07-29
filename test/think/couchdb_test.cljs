(ns test.think.couchdb-tests
  (:refer-clojure  :exclude [create-node])
  (:require
    [think.util.core    :as util]
    [think.couchdb      :as db]
    [think.object       :as object]
    [think.util.log     :refer (log log-obj)]
    [cljs.core.async    :refer (chan >! <! close!)])
  (:require-macros
    [test.think.helpers :refer [is= is deftest testing runner]]
    [cljs.core.async.macros :refer (go)]))



    
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


