(ns think.cljs.test.defonce
	(:require-macros [think.macros :refer [defonce]]
									 [cemerick.cljs.test :refer [is deftest]])
	(:require [think.util.log :refer [log log-obj]]
				    [think.cljs.test :refer [test-ns]]))


; (defonce ::foo "bar")

; (defonce ::foo "woz")

; (deftest test-defonce
; 	(is
; 		(= foo "bar")))



;; test that defonce allows for new defs in other namespaces
(ns think.cljs.test.defonce2
	(:require-macros [think.macros :refer [defonce]]
									 [cemerick.cljs.test :refer [is deftest]])
	(:require [think.util.log :refer [log log-obj]]
				    [think.cljs.test :refer [test-ns]]))

; (defonce ::foo "woz")

; (deftest test-defonce
; 	(is
; 		(= foo "woz"))
; 	(is
; 		(not= foo "bar")))




(comment

	;; run tests by loading code into repl then running
	(test-ns 'think.cljs.test.defonce)
	)