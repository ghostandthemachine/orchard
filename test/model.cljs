(ns test.model
  (:require [think.util.core    :as util]
            [think.couchdb      :as db]
            [think.model        :as model]
            [think.util.log     :refer [log log-obj]]
            [redlobster.promise :as p]
            [cemerick.cljs.test :refer [test-ns]])
  (:require-macros [cemerick.cljs.test :refer [deftest testing is use-fixtures run-tests]]
                   [redlobster.macros  :refer [when-realised let-realised defer-node]]))


(defn test-doc
  []
  {:type :test-document
   :_id  (util/uuid)
   :body "This is a test body"})


; (deftest save-doc-test
;   (let [doc   (test-doc)
;         okeys (keys doc)]
;     (let-realised [sd (model/save-document doc)]
;       (let-realised [gd (model/get-document (:id doc))]
;         (testing "testing save-document"
;           (is (= doc (select-keys @gd (keys doc)))))))))


(defn teardown-test
  []
  (log "Destroying testing database")
  (db/delete-db "testing")
  (log "Loading projects database")
  (model/load-db))


(defn setup-test
  []
  (log "Creating test database")
  (model/load-db "testing"))


(use-fixtures :once
  (fn [f]
    (println "Setting up tests...")
    (setup-test)
    (f)
    (teardown-test)))


(deftest testing-testing
  (is (= 1 1)))


(deftest testing-testing2
  (is (= 1 0)))


(defn run-tests
  []


  (test-ns 'test.model))
