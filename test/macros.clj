(ns test.macros)

(defmacro is
  [pred-form & [msg]]
  `(if ~pred-form
     (test.core/report {:type :pass :msg ~msg})
     (test.core/report {:type :fail :msg ~msg})))


