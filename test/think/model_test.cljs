(ns test.model
  (:require [think.util.core    :as util]
            [think.couchdb      :as db]
            [think.model        :as model]
            [think.util.log     :refer (log log-obj)]))

;
;(defn test-doc
;  []
;  {:type :test-document
;   :id  (util/uuid)
;   :body "This is a test body"})
;
;
;(deftest save-doc-test
;   (let [doc   (test-doc)
;         okeys (keys doc)]
;     (log (str "current database: " @think.model/model-db*))
;     (let-realised [sd (model/save-document doc)]
;       (let-realised [gd (model/get-document (:id doc))]
;         (testing "testing save-document"
;           (is (= doc (select-keys @gd (keys doc)))))))))
;
;
;(defn teardown-test
;  []
;  (log "Destroying testing database")
;  ;(db/delete-db "testing")
;  (log "Loading projects database")
;  (model/load-db))
;
;
;(defn setup-test
;  []
;  (log "Creating test database")
;  (model/load-db "testing"))
;
;
;(use-fixtures :once
;  (fn [f]
;    (let [db (setup-test)]
;      (p/on-realised db
;        (fn on-success []
;          (f)
;          (teardown-test))
;        (fn on-error []
;          (log "Error trying to load testing database"))))))
;
;
;(deftest testing-promise
;  (let-realised [d (model/get-document :home)]
;    (log "realizing document in test")
;    (log-obj @d)
;    (is (= @d @d))))
;
;(deftest testing-1
;  (is
;    (= 1 1)))
;
;
;(defn run-tests
;  []
;  (test-ns 'test.model))
