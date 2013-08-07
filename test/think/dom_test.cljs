(ns think.test.dom-test
  (:require-macros
    [test.helpers :refer (deftest is is= testing)])
  (:require
    [think.util.core :refer [uuid]]
    [think.util.dom :refer (remove sdata gdata by-id by-class add-id create append)]))


(defn add-test-elem
  []
  "Creates a test node and appends it to the DOM. Returns the created node and a unique id."
  (let [elem    (create :div)
        elem-id (uuid)]
    (add-id elem "test-node")
    (append js/document.body elem)
    [elem elem-id]))



(defn all-tests
  []
  (deftest selector-test
    (let [[elem elem-id] (add-test-elem)]
      (testing "should select a DOM node by id."
        (is=
          elem
          (by-id elem-id)))
      (remove elem)))


  (deftest data-attribute-test
    (let [data           {"foo" "bar"
                          "biz" "boz"}
          [elem elem-id] (add-test-elem)]
      (testing "should set and get data attributes from a DOM node."
        (sdata elem "test-data" data)
        (is= {"boner" "loner"} (gdata elem "test-data"))))))


(defn run-tests
  []
  (all-tests))