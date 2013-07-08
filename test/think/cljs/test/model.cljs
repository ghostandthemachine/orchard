(ns think.cljs.test.model
  (:refer-clojure  :exclude [create-node])
  (:require [think.util.core    :as util]
            [think.util.time    :as time]
            [think.couchdb      :as db]
            [think.object       :as object]
            [think.model        :as model]
            [redlobster.promise :as p]
            [think.util.log     :refer [log log-obj]]
            [cemerick.cljs.test :as t])
  (:require-macros [cemerick.cljs.test :refer [is deftest with-test run-tests testing use-fixtures]]
                   [redlobster.macros  :refer [when-realised let-realised defer-node]]
                   [think.macros       :refer [defui]]))


(defn teardown-test
  []
  (object/raise model/model :destroy-db "testing")
  (log "Destroyed testing database")
  (object/raise model/model :load-db "projects")
  (log "Loaded projects database"))


(defn setup-test
  []
  (object/raise model/model :load-db "testing")
  (log "Created test database"))


(defn test-doc
  []
  {:type :test-document
   :_id  (util/uuid)
   :body "This is a test body"})


;*
; Testing Code
;*

(use-fixtures :once
  (fn [f]
    (println "Setting up tests...")
    (setup-test)
    (f)
    (teardown-test)))


(def test-doc-ids* (atom (map #(test-doc) (range 10))))


(with-test
  (defn save-document
    [doc]
    (model/save-document doc))
  (let [doc   (test-doc)
        okeys (keys doc)]
    (let-realised [d (save-document doc)]
      (println "document " d)
      (testing "testing save-document"
        (is (= doc (select-keys @d okeys)))))))

(let-realised [p (p/promise)])

(comment

(t/test-ns 'think.cljs.test.model)

  )