(ns orchard.objects.d3
  (:use-macros [orchard.macros :only [defui]])
  (:require [orchard.object :as object]
            [crate.core :as crate]
            [orchard.util.dom :as dom]
            [orchard.util.core :refer [bound-do uuid]]
            [orchard.module :refer [default-opts edit-module-btn-icon delete-btn edit-btn]]
            [orchard.util.log :refer (log log-obj)]
            [crate.binding :refer [bound subatom]]
            [dommy.core :as dommy]))


; (def d3 js/d3)


; (def width  450)
; (def height 450)
; (def radius 225)

; (def color
;   (.range
;     (.ordinal (.-scale d3))
;       ["#98abc5"
;        "#8a89a6"
;        "#7b6888"
;        "#6b486b"
;        "#a05d56"
;        "#d0743c"
;        "#ff8c00"]))

; (def arc  ;; based on (http://bl.ocks.org/mbostock/3887193) example, this generates a function
;   (-> (.arc (.-svg d3))
;     (.outerRadius (- radius 10))
;     (.innerRadius (- radius 70))))


; (def pie  ;; based on (http://bl.ocks.org/mbostock/3887193) example, this generates a function
;   (.value
;     (.sort (js/d3.layout.pie) null)
;     (fn [d] (set! (.-population d) (js/Math.abs (.-population d))))))


; (defn add-svg
;   [parent-selector]
;   (let [svg (-> d3
;               (.select (name parent-selector))
;               (.append "svg"))]
;     (-> d3
;       (.attr "width" width)
;       (.attr "height" height)
;       (.attr "transform"
;         (str "translate(" radius "," radius ")")))))


; (defn render-pie-chart
;   [this]
;   (let [svg-selector (str "#chart-conatiner-" (:id @this))
;         _            (add-svg svg-selector)
;         $svg         (dom/$ svg-selector)]
;     (.csv d3 "data.csv"
;       (fn [error data]
;         (.forEach data
;           #(set! (.-population %) (js/Math.abs (.-population %))))
;         (let [g (->
;                   (.selectAll $svg ".arc")
;                   (.data (pie data))
;                   (.enter)
;                   (.append "g")
;                   (.attr "class" "arc"))]
;           (->  ;; create path element
;             (.append g "path")
;             (.attr "d" arc)
;             (.style "fill"
;               (fn [d] (color (.-age (.-data d))))))
;           (->  ;; create text element
;             (.append g "text")
;             (.attr "transform" #(str "traslate(" (.cenrtoid arc %) ")"))
;             (.attr "dy" ".35em")
;             (.attr "text-anchor" "middle")
;             (.text #(.-age (.-data %)))))))))


; (defn d3-chart-doc
;   []
;   {:type   :pie-chart
;    :width  width
;    :height height
;    :radius radius})


; (object/object* :pie-chart
;   :tags #{}
;   :behaviors []
;   :init (fn [this]
;           [:div.span12.module.html-module {:id (str "module-" (:id @this)) :draggable "true"}
;             [:div.module-tray (delete-btn this) (edit-btn this)]
;             [:div.module-element
;               [:div {:id (str "#chart-conatiner-" (:id @this))}
;                 (render-pie-chart this)]]]))

