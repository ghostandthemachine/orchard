(ns test.think.couchdb-tests
  (:refer-clojure  :exclude [create-node])
  (:require
    [think.util.core    :as util]
    [think.couchdb      :as db]
    [think.object       :as object]
    [think.util.log     :refer [log log-obj]]
    [cljs.core.async    :refer (chan >! <! close!)]
    [cemerick.cljs.test :refer [test-ns]])
  (:require-macros
    [test.helpers :as h :refer [is= is deftest testing runner]]
    [cljs.core.async.macros :refer (go alt! alts!)]))


(deftest couch-ids-test
  (testing "should convert both id and rev keys to _id and _rev"
    (is= (db/couch-ids {:id "test-id" :rev "rev-id"}) {"_id" "test-id" "_rev" "rev-id"})))


(deftest cljs-ids-test
  (testing "should convert both \"_id\" and \"_rev\" keys to :id and :rev"
    (is= (select-keys (db/cljs-ids {:_id "test-id" :_rev "rev-id"})
                      [:id :rev])
         {:id "test-id" :rev "rev-id"})))


(deftest list-all-test
  (testing "should look up all available couch databases"
    (go
      (is=
        ["asd_replicator" "_users" "projects"]
        (:value (<! (db/list-all)))))))




