(ns cemerick.cljs.test
  (:require cljs.compiler
            [cemerick.cljs.test :refer [assert-expr assert-predicate assert-any function?]]
            [cljs.analyzer :refer (*cljs-ns* get-expander)]
            [clojure.template :as temp]))


; (defn promise?
;   [v]
;   (= redlobster.promise/Promise (type v)))


; (defmethod assert-expr :default [msg form]
;   (if (and (seq? form) (function? (first form)))
;     (assert-predicate msg form)
;     (assert-any msg form)))


;; https://github.com/cemerick/clojurescript.test/blob/master/src/cemerick/cljs/test.clj#L287
; (defmethod assert-expr :promise
;   [msg form]
;   (let [[promises non-promises] (reduce
;                                   (fn [[ps nps] v]
;                                     (if (promise? v)
;                                       [(conj ps v) nps]
;                                       [ps (conj nps v)]))
;                                   form)]
;     ;; not really sure how to parse promises from non promises
;     ;; something like
;     (util/await [resolved-form promises]
;       ;; and then how to reconsturct the original form as all realized values
;       ;; 
;       ;; then I would call something like
;       (if (and (seq? resolved-form) (function? (first resolved-form)))
;         (assert-predicate msg resolved-form)
;         (assert-any msg resolved-form)))))



; (defmethod assert-expr :promise
;   [msg form]
;   (println msg form)
;   (if (and (seq? form) (t/function? (first form)))
;     (t/assert-predicate msg form)
;     (t/assert-any msg form)))

; (comment



; (deftest promise-test
;   (let [foo-promise (p/promise)]
;     (p/realise foo-promise "foo")
;     (is :promise  ;; the is macro can take an optional type to allow for cutom assert-expr's https://github.com/cemerick/clojurescript.test/blob/master/src/cemerick/cljs/test.clj#L372
;       (=
;         "foo"
;         foo-promise)))
;   "Should take an assertion form, resolve any promises 
;   in it, and then apply the assertion to the seq.")


; )