(ns think.css
  ; (:refer-clojure :exclude [+])
  (:require [think.util.log :refer [log log-obj]]))

; (deftype HexColor [value])

; (deftype RGBAColor [r g b a])

; (defmulti color type)

; (defmethod color js/String
;   [hex-color]
;   (HexColor. (name hex-color)))

; (defmethod color PersistentVector
;   [[r g b & r]]
;   (RGBAColor. r g b (or (first r) 255)))



; (defmulti + :type)

; ; (defmethod + :default
; ;   [v & args]
; ;   (apply clojure.core/+ args))

; (defmethod + :px
;   [px & args]
;   (println "px ")
;   (log-obj (:value px))
;   (apply clojure.core/+ args)
;   )

