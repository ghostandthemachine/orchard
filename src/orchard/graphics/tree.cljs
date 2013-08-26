(ns orchard.graphics.tree
  (:require
    [orchard.util.log :refer (log log-obj)]))


(defn deg->radian
  [deg]
  (* deg Math/PI (/ 1.0 180)))


(defn cos-deg
  [angle]
  (Math/cos (deg->radian angle)))


(defn sin-deg
  [angle]
  (Math/sin (deg->radian angle)))


(defn draw-tree*
  [ctx x y angle depth]
  (when (pos? depth)
    (let [x2 (+ x (int (* depth 6 (cos-deg angle))))
          y2 (+ y (int (* depth 6 (sin-deg angle))))]
      (.beginPath ctx)
      (.moveTo ctx x y)
      (.lineTo ctx x2 y2)
      (.closePath ctx)
      (.stroke ctx)
      (draw-tree* ctx x2 y2 (- angle 20) (dec depth))
      (recur      ctx x2 y2 (+ angle 20) (dec depth)))))


(defn draw-tree
  [canvas-id]
  (let [target (.getElementById js/document canvas-id)
        context (.getContext target "2d")]
    (draw-tree* context
                (int (/ (.-width target) 2.0))
                (int (/ (.-height target) 2.0))
                -90 9)))


