(ns test.kv_store
  (:require-macros
    [test.helpers :refer (is= is deftest testing runner throws?)])
  (:require [orchard.kv-store :as kv]
            [orchard.util.log   :refer (log log-obj)]))

(def test-items
  {:foo "asdf"
   :bar 23
   :baz {:name "asdf"}
   :buz [1.2 3.4]
   })


(deftest local-storage
  (let [size (kv/local-count)]
    (doseq [k (keys test-items)]
      (kv/local-set k (test-items k)))

    (doseq [k (keys test-items)]
      (is= (test-items k) (kv/local-get k)))

    (is= (+ size (count (keys test-items))) (kv/local-count))

    (doseq [k (keys test-items)]
      (kv/local-remove k))

    (is= size (kv/local-count))))

