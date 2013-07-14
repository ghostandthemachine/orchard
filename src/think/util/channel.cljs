(ns think.util.channel)
	; (:require [clojure.core.async :refer [go chan >! <! put! timeout alts!]]))


; (defn event-chan [el type]
;   (let [c (chan)]
;     (.addEventListener el type #(put! c %))
;     c))

; (defn map-chan [f in]
;   (let [c (chan)]
;     (go (loop []
;           (>! c (f (<! in)))
;           (recur)))
;     c))