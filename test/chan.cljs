(ns test.chan
  (:require
    [cljs.core.async :refer (chan >! <! close!)]
    [think.util.log  :refer (log log-obj)]
    [clojure.string  :as    string]
    [test.core       :refer (test-async)])
  (:require-macros
    [test.macros :refer (is)]
    [cljs.core.async.test-helpers :as h :refer [is= is deftest testing runner]]
    [cljs.core.async.macros :refer (go alt! alts!)]))
 

;(def loc-div (.getElementById js/document "container"))
;
;(defn test-event-chan
;  []
;  (.addEventListener
;    js/window "mousemove"
;    (fn [e]
;      (go
;        (>! c [(.-x e) (.-y e)]))))
;
;  (go
;    (while true
;      (let [loc (<! c)]
;        (log "location: " loc)))))
;
;(defn foo []
;  (let [c (chan)]
;    (thread (>!! c "hello"))
;    (log "Got message from channel: ")
;    (log-obj (<!! c))
;    (close! c)))


(defn fake-test
  []
  (test-async
    (go
      (let [c (chan 1)
            _ (>! c :foo)
            v (<! c)]
        (is (= :foo v))))))


