(ns test.model
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [test.helpers :refer (is= is deftest testing runner throws?)])
  (:require [cljs.core.async    :refer (chan >! <! put! take!)]
            [orchard.util.core  :as util]
            [orchard.model      :as model]
            [orchard.util.log   :refer (log log-obj)]))


(defn test-doc
  []
  {:type :test-document
   :id  (util/uuid)
   :body "This is a test body"})

(defn all-tests
  []
  (deftest save-object-test
    (let [doc   (test-doc)
          okeys (keys doc)
          db    (model/local-store)]
      (go
        (let [nil-obj (<! (model/get-object db (:id doc)))
              _ (is= nil nil-obj)
              ;_ (model/save-object! db (:id doc) doc)
              obj (<! (model/get-object db (:id doc)))]
          (log "checking keys...")
          (doseq [k okeys]
            (log (str k ": " (obj k)))
            (is= (obj k) (doc k)))))))

  (log "model tests complete..."))

(all-tests)
