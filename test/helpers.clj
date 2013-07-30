(ns test.helpers
  (:require [cljs.core.async.impl.ioc-macros :as ioc]))


(defmacro test-assert
  "Evaluates expr and throws an exception if it does not evaluate to
  logical true."
  ([x]
     (when *assert*
       `(when-not ~x
          (.write js/process.stdout "ASSERT HERE----------------")
          (let [err-msg# (cljs.core/str "Assert failed: " (cljs.core/pr-str '~x))]
            (.write js/process.stdout (str err-msg# "\n"))
            (throw (js/Error. err-msg#))))))
  ([x message]
     (when *assert*
       `(when-not ~x
          (let [err-msg# (cljs.core/str "Assert failed: " ~message "\n" (cljs.core/pr-str '~x))]
            (.write js/process.stdout (str err-msg# "\n"))
            (throw (js/Error. err-msg#)))))))


(defmacro runner
  "Creates a runner block. The code inside the body of this macro will be translated
  into a state machine. At run time the body will be run as normal. This transform is
  only really useful for testing."
  [& body]
  (let [terminators {'pause 'cljs.core.async.runner-tests/pause}]
    `(let [state# (~(ioc/state-machine body 0 &env terminators))]
       (cljs.core.async.impl.ioc-helpers/run-state-machine state#)
       (assert (cljs.core.async.impl.ioc-helpers/finished? state#) "state did not return finished")
       (aget state# ~ioc/VALUE-IDX))))


(defmacro deftest
  [nm & body]
  `(do (.write js/process.stdout (str "Testing: " ~(str nm) "...\n"))
       ~@body))


(defmacro testing
  [nm & body]
    `(do (.write js/process.stdout (str "    " ~nm "...\n"))
       ~@body))


(defmacro is=
  [a b]
  `(let [a# ~a
         b# ~b]
     (test-assert (= a# b#) (str a# " != " b#))))


(defmacro is
  [a]
  `(test-assert ~a))
