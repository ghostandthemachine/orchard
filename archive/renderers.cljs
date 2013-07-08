(ns think.renderers
  (:use-macros [dommy.macros :only [sel]]))

;====================================================================================
; Node renderers
;====================================================================================


(defn- circle-renderer
  [graphics selected? [px py] {alpha :alpha shape :shape color :color node-name :name text-attributes :text-attributes}]
  (let [w (Math/max 40 (+ 40 (.textWidth graphics node-name)))]
    (when selected?
      (doto graphics
        (.oval (- px (/ w 2) 3) (- py (/ w 2) 3) (+ 6 w) (+ 6 w) (clj->js {:stroke color
                                                                           :width 2
                                                                           :alpha alpha}))))

      (doto graphics
        (.oval (- px (/ w 2)) (- py (/ w 2)) w w (clj->js {:fill color
                                                           :alpha alpha}))
        (.text node-name px (+ py 7) (clj->js text-attributes)))))

(defn- rect-renderer
  [graphics selected? [px py] {alpha :alpha shape :shape color :color node-name :name text-attributes :text-attributes}]
  (let [w (Math/max 40 (+ 40 (.textWidth graphics node-name)))
        h 30]
    (when selected?
      (doto graphics
        (.rect (- px (/ w 2) 3) (- py (/ h 2) 3) (+ 6 w) (+ 6 h) 7 (clj->js {:stroke color
                                                                               :width 2
                                                                               :alpha alpha}))))

      (doto graphics
        (.rect (- px (/ w 2)) (- py (/ h 2)) w h 5 (clj->js {:fill color
                                                               :alpha alpha}))
        (.text node-name px (+ py 7) (clj->js text-attributes)))))


(defn- image-renderer
  [graphics selected? [px py] {alpha :alpha shape :shape color :color node-name :name text-attributes :text-attributes img :img}]
  (let [w (/ (.-width img) 10)
        h (/ (.-height img) 10)
        ctx (.getContext (first (sel "#mind-map")) "2d")]
    (when selected?
      (doto ctx
        (.rect (- px (/ w 2) 3) (- py (/ h 2) 3) (+ 6 w) (+ 6 h) 7 (clj->js {:stroke color
                                                                               :width 2
                                                                               :alpha alpha}))))

      (doto ctx
        (.drawImage img (- px (/ w 2)) (- py (/ h 2)) w h))))