(ns test.core
  (:require-macros [redlobster.macros :refer [let-realised]])
  (:require [cemerick.cljs.test :refer [do-report *testing-vars* 
                                        inc-report-counter *initial-report-counters* 
                                        registered-fixtures join-fixtures 
                                        registered-tests registered-test-hooks 
                                        *report-counters*]]
            [clojure.string :as str]))


(def TESTED-NAMESPACES ['test.think.model])

(defn promise?
  [v]
  (= redlobster.promise/Promise (type v)))


;;; RUNNING TESTS: LOW-LEVEL FUNCTIONS
;; TODO since there's no vars, rename these helpers to *-fn?
(defn test-var
  "If v has a function in its :test metadata, calls that function,
  with *testing-vars* bound to (conj *testing-vars* v)."
  {:dynamic true, :added "1.1"}
  [v]
  (assert (fn? v) "test-var must be passed the function to be tested (not a symbol naming it)")
  (when-let [t (:test (meta v))]
    (binding [*testing-vars* (conj *testing-vars* (or (:name (meta v)) v))]
      (do-report {:type :begin-test-var, :var v})
      (inc-report-counter :test)
      (try
        (let [return (t)]
          (if (promise? return)
            (let-realised [v return]
              (do-report {:type :pass :var v}))
            return))
           (catch js/Error e
             (do-report {:type :error, :message "Uncaught exception, not in assertion."
                      :expected nil, :actual e})))
      (do-report {:type :end-test-var, :var v}))))


(defn test-all-vars
  "Calls test-var on every var interned in the namespace, with fixtures."
  {:added "1.1"}
  [ns-sym]
  (let [once-fixture-fn (-> @registered-fixtures ns-sym :once join-fixtures)
        each-fixture-fn (-> @registered-fixtures ns-sym :each join-fixtures)]
    (once-fixture-fn
     (fn []
       (doseq [v (get @registered-tests ns-sym)]
         (when (:test (meta v))
           (each-fixture-fn (fn [] (test-var v)))))))))


(defn test-ns
  "If the namespace defines a function named test-ns-hook, calls that.
  Otherwise, calls test-all-vars on the namespace.  'ns' is a
  namespace object or a symbol.

  Internally binds *report-counters* to an atom initialized to
  *initial-report-counters*.  Returns the final, dereferenced state of
  *report-counters*."
  {:added "1.1"}
  [ns-sym]
  (binding [*report-counters* (atom *initial-report-counters*)]
    (do-report {:type :begin-test-ns, :ns ns-sym})
    ;; If the namespace has a test-ns-hook function, call that:
    (if-let [test-hook (get @registered-test-hooks ns-sym)]
      (test-hook)
      ;; Otherwise, just test every var in the namespace.
      (test-all-vars ns-sym))

    (do-report {:type :end-test-ns, :ns ns-sym})
    @*report-counters*))


(defn run-all-tests
  []
  (doseq [testing-ns TESTED-NAMESPACES]
    (test-ns testing-ns)))
