(ns test.orchard.macros
  (:require-macros [orchard.macros :refer [defonce]])
  (:require [orchard.util.log :refer (log log-obj)]
            [test. :refer [test-ns]]))


; (defonce ::foo "bar")

; (defonce ::foo "woz")

; (deftest test-defonce
; 	(is
; 		(= foo "bar")))



;; test that defonce allows for new defs in other namespaces
; (ns test.defonce2
; 	(:require-macros [orchard.macros :refer [defonce]]
; 									 [cemerick.cljs.test :refer [is deftest]])
; 	(:require [orchard.util.log :refer (log log-obj)]
; 				    [test. :refer [test-ns]]))

; (defonce ::foo "woz")

; (deftest test-defonce
; 	(is
; 		(= foo "woz"))
; 	(is
; 		(not= foo "bar")))

