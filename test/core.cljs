(ns test.core)

(def test-results* (atom {:pass 0 :fail 0 :error 0}))
(def async-tests* (atom []))


(defn test-async
  [c]
  (swap! async-tests* conj c))


(defn inc-report
  [t]
  (swap! test-results* #(assoc % t (inc (t %)))))


(defn report
  [res]
  (inc-report (:type res)))


