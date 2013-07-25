(ns test.think.couchdb
  (:refer-clojure  :exclude [create-node])
  (:require [think.util.core       :as util]
            [think.couchdb         :as db]
            [think.object          :as object]
            [test..model :as test-model]
            [redlobster.promise    :as p]
            [think.util.log        :refer [log log-obj]]
            [test.       :refer [test-ns]])
  (:require-macros [cemerick.cljs.test :refer [is deftest with-test run-tests testing use-fixtures]]
                   [redlobster.macros  :refer [when-realised let-realised defer-node]]
                   [think.macros       :refer [defui]]))


(deftest couch-ids
  (is
    (= (db/couch-ids {:id "test-id" :rev "rev-id"}) {"_id" "test-id" "_rev" "rev-id"})
    "should convert both id and rev keys to _id and _rev"))


(deftest cljs-ids
  (is
    (=
      (select-keys
        (db/cljs-ids {"_id" "test-id" "_rev" "rev-id"}) [:id :rev])
      {:id "test-id" :rev "rev-id"})
    "should convert both \"_id\" and \"_rev\" keys to :id and :rev"))

;(test-ns 'test..couchdb)