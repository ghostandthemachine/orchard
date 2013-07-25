(ns think.util.channel
  (:require [cljs.core.async :refer [go chan >! <! put! timeout alts!]]))


(defn event-chan
  "Returns a channel that will receive events of type e-type emitted by dom
  element el."
  [el e-type]
  (let [c (chan)]
    (.addEventListener el e-type #(put! c %))
    c))


(defn map-chan
  "Create a channel that will output the result of calling function f on
  each value put onto the input channel."
  [f in]
  (let [c (chan)]
    (go (loop []
          (>! c (f (<! in)))
          (recur)))
    c))


(defn chan-callback
  "Returns a callback that writes the value passed to itself to a channel."
  [c]
  (fn [v] (>! c v)))
