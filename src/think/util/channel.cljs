(ns think.util.channel
  (:require [cljs.core.async :refer [go chan >! <! put! timeout alts!]]
  					[think.util.dom :refer [on-event $ prevent]]
  					[dommy.core :as dommy]))


(defn event-chan [el type]
  (let [c (chan)]
    (.addEventListener el type #(put! c %))
    c))


(defn map-chan [f in]
  (let [c (chan)]
    (go (loop []
          (>! c (f (<! in)))
          (recur)))
    c))


(defn data-from-event
	[e]
	e)


(defn on
	([ev-type selector callback]
		(on ($ :body) ev-type selector callback))
	([parent-el ev-type selector callback]
		(dommy/listen! [(dom/$ :body) selector] ev-type callback)))


(defn click-chan
	[selector msg-name]
	(let [rc (chan)]
		(on ($ :body) :click selector
			(fn [e]
				(prevent e)
				(put! rc [msg-name (data-from-event e)])))))


(defn offset [el]
  (fn [e]
    {:x (- (.-pageX e) (.-offsetLeft el))
     :y (- (.-pageY e) (.-offsetTop el))}))

; (let [el  ($ :ex2)
;       out ($ :ex2-mouse)
;       c   (map-chan (offset el)
;             (event-chan el "mousemove"))]
;   (go (loop []
;         (let [e (<! c)]
;           (set-html out (str (:x e) ", " (:y e)))
;           (recur)))))