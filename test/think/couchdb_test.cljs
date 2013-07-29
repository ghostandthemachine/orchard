(ns test.thnk.couchdb
  (:refer-clojure  :exclude [create-node])
  (:require
    [think.util.core    :as util]
    [think.couchdb      :as db]
    [think.object       :as object]
    [think.util.log     :refer [log log-obj]]
    [cljs.core.async    :refer (chan >! <! close!)]
    [cemerick.cljs.test :refer [test-ns]])
  (:require-macros
    [cljs.core.async.test-helpers :as h :refer [is= is deftest testing runner]]
    [cljs.core.async.macros :refer (go alt! alts!)]))


(deftest couch-ids-test
  (is
    (= (db/couch-ids {:id "test-id" :rev "rev-id"}) {"_id" "test-id" "_rev" "rev-id"})
    "should convert both id and rev keys to _id and _rev"))


(deftest cljs-ids-test
  (is= (select-keys (db/cljs-ids {:_id "test-id" :_rev "rev-id"})
                    [:id :rev])
       {:id "test-id" :rev "rev-id"})
  "should convert both \"_id\" and \"_rev\" keys to :id and :rev")


(deftest list-all-test
  (go
    (is= ["foo" "bar"] (<! (db/list-all)))))

