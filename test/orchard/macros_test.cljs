(ns test.orchard.macros
  (:require-macros [orchard.macros :refer [def]])
  (:require [orchard.util.log :refer (log log-obj)]
            [test. :refer [test-ns]]))


; (def ::foo "bar")

; (def ::foo "woz")

; (deftest test-def
; 	(is
; 		(= foo "bar")))



;; test that def allows for new defs in other namespaces
; (ns test.def2
; 	(:require-macros [orchard.macros :refer [def]]
; 									 [cemerick.cljs.test :refer [is deftest]])
; 	(:require [orchard.util.log :refer (log log-obj)]
; 				    [test. :refer [test-ns]]))

; (def ::foo "woz")

; (deftest test-def
; 	(is
; 		(= foo "woz"))
; 	(is
; 		(not= foo "bar")))

