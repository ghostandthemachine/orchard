(ns test.chan
  (:require [cljs.core.async :refer (chan >!! <!! close! thread)]
            [think.util.log :refer (log log-obj)]
            [clojure.string :as string])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt! alts!]]))

;(def c (chan))
;(def loc-div (.getElementById js/document "container"))
;
;(.addEventListener
;  js/window "mousemove"
;  (fn [e]
;    (go
;      (>! c [(.-x e) (.-y e)]))))
;
;(go
;  (while true
;    (let [loc (<! c)]
;      (log "location: " loc))))
;(def c (chan 2))
;
;(defn a []
;  (let [c (chan)]
;    (thread (>!! c "hello"))
;    (log "Got message from channel: ")
;    (log-obj (<!! c))
;    (close! c)))
