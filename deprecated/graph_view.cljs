(ns think.graph-view
  (:use-macros [dommy.macros :only [sel]])
  (:use [think.util :only [log jslog uuid]]
        [think.renderers :only [circle-renderer rect-renderer image-renderer]]
        [think.canvas :only [text line rect oval clear]])
  (:require [think.dispatch :as dispatch]
            [think.graph-model :as model]))


; (def ^{:private true} state (atom {}))

; (defn get-canvas [] (:canvas @state))
; (defn get-graphics-context [canvas] (.getContext canvas "2d"))
; (defn get-graphics [] (:graphics @state))
; (defn get-particle-system [] (:particle-system @state))

; (defn set-nearest-node! [id] (swap! state assoc :nearest id))
; (defn get-nearest-node [] (:nearest @state))

; (defn set-cursor! [id] (swap! state assoc :cursor id))
; (defn get-cursor [] (:cursor @state))

; (defn set-selected-node! [id] (swap! state assoc :selected id))
; (defn get-selected-node [] (:selected @state))

; (defn set-current-node! [id] (swap! state assoc :current id))
; (defn get-current-node [] (:current @state))

; (defn dragging? [] (:dragging? @state))
; (defn set-dragging! [b] (swap! state assoc :dragging? b))

; (defn over-node? [] (not (nil? (:current @state))))

; (defn get-node-id [n] (.-name (.-node n)))
; (defn get-node-name [n] (model/get-node-name (get-node-id n)))

; (defn show-grid? [] (== (:toggle-grid? @state) true))
; (defn show-fps? [] (== (:toggle-fps? @state) true))

; (defn selected? [id]
;   (when-let [selected (get-selected-node)]
;     (= id (.-name (.-node selected)))))

; ;====================================================================================
; ; Drawing
; ;====================================================================================

; (defn- draw-grid []
;   (let [canvas (get-canvas)
;         graphics (get-graphics)
;         w (.-width canvas)
;         h (.-height canvas)]
;     (doseq [i (range (/ w 30))]
;       (.line graphics (.Point js/arbor (* 30 i) 0) (.Point js/arbor (* 30 i) h) (clj->js {:stroke "black"
;                                              :alpha 0.1 :width 1}))
;       )
;     (doseq [i (range (/ h 30))]
;       (.line graphics (.Point js/arbor 0 (* 30 i)) (.Point js/arbor w (* 30 i)) (clj->js {:stroke "black"
;                                              :alpha 0.1 :width 1})))))

; (defn redraw-graph []
;   (let [particle-system (get-particle-system)
;         graphics (get-graphics)
;         ctx (get-graphics-context (get-canvas))]
;     (.clear graphics)
;     (when (show-grid?)
;       (draw-grid))

;     (when (show-fps?)
;       (.text graphics (str "FPS: " (.fps particle-system)) 9 23 (clj->js {:color "black"
;                                                                          :align "left"
;                                                                          :font "Arial"
;                                                                          :size 12})))

;     (.eachEdge particle-system
;       (fn [edge  p1  p2]
;         (when-let [target (.-name (.-target edge))]
;           (.line graphics p1 p2 {:stroke "#CCCCCC"
;                                  :width 2
;                                  :alpha 1}))))

;     (.eachNode particle-system
;       (fn [n pos]
;         (let [node-id (.-name n)
;               data (model/get-node node-id)
;               ; data (merge (model/get-node node-id) {:color (.-color n) :alpha (.-alpha n)})
;               renderer (:renderer data)
;               node-position [(.-x pos) (.-y pos)]]
;         (case renderer
;           :circle (circle-renderer graphics (selected? node-id) node-position data)
;           :rect   (rect-renderer graphics (selected? node-id) node-position data)
;           :image  (image-renderer graphics (selected? node-id) node-position data)
;           (circle-renderer graphics (selected? node-id) node-position data)))))))

; (defn resize-graph []
;   (let [canvas (get-canvas)
;         particle-system (get-particle-system)]
;     (set! (.-width canvas) (.-innerWidth js/window))
;     ; (set! (.-height canvas) (* 0.75 (.-innerHeight js/window)))
;     (.screenSize particle-system (.-width canvas) (.-height canvas))
;     (redraw-graph)))

; ; ;===============================================================================
; ; ; Mouse handler functions
; ; ;===============================================================================

; (defn- mousedragged-handler [e]
;   (let [selected (.-node (get-selected-node))
;         $canvas ($ (get-canvas))
;         pos (.offset $canvas)
;         point (.Point js/arbor (- (.-pageX e) (.-left pos)) (- (.-pageY e) (.-top pos)))
;         particle-system (get-particle-system)
;         p (.fromScreen particle-system point)]
;       (set! (.-p selected) p)))

; (defn- mousemoved-handler [e]
;   (let [pos (.offset ($ (get-canvas)))
;         particle-system (get-particle-system)]
;     (set-cursor! (.Point js/arbor (- (.-pageX e) (.-left pos)) (- (.-pageY e) (.-top pos))))
;     (set-nearest-node! (.nearest particle-system (get-cursor)))
;     (when-let [nearest  (get-nearest-node)]
;       (if (< (.-distance nearest) 40)
;         (set-current-node! nearest)
;         (set-current-node! nil))))
;     (redraw-graph)
;     false)

; (defn- mouseup-handler [e]
;   (when (dragging?)
;     (let [n (.-node (get-selected-node))
;           id (.-name n)
;           original-fixed (:fixed (model/get-node id))]
;       (set! (.-fixed n) original-fixed)
;       (set! (.-tempMass n) 1000))
;     (dispatch/fire :node-dropped))

;   (set-dragging! false)
;   (.unbind ($ (get-canvas)) "mousemove" mousedragged-handler)
;   (.mousemove ($ (get-canvas)) "mousemove" mousemoved-handler)
;   false)

; (defn- mousedown-handler [e]
;   (let [current (get-current-node)]
;     (if (over-node?)
;       (do
;         (set-dragging! true)
;         (set-selected-node! (get-current-node))

;         (let [n (.-node (get-selected-node))]
;           (set! (.-fixed n) true))

;         (.unbind ($ (get-canvas)) "mousemove" mousemoved-handler)
;         (.mousemove ($ (get-canvas)) "mousemove" mousedragged-handler)

;         (dispatch/fire :node-selected (get-node-id (get-selected-node))))
;       (do
;         (set-dragging! false)
;         (set-selected-node! nil)
;         (dispatch/fire :nothing-selected))))
;   (redraw-graph))

; (defn- init-mouse-handlers []
;   (let [$canvas  ($ (get-canvas))]
;     (.mousedown $canvas mousedown-handler)
;     (.mousemove $canvas mousemoved-handler)
;     (.mouseup $canvas mouseup-handler)
;     (.mouseout $canvas mouseup-handler))
;   false)

; (defn- init-graph []
;   (let [canvas (get-canvas)
;         particle-system (get-particle-system)
;         $window ($ js/window)]
;     (.screenSize particle-system (.-width canvas) (.-height canvas))
;     (.screenPadding particle-system 80)
;     (.resize $window resize-graph)
;     (resize-graph)
;     (init-mouse-handlers)))

; (defn create-renderer [elt]
;   (let [canvas (first (sel "#graph-canvas"))
;         graphics (.Graphics js/arbor canvas)
;         graph-handlers {:init init-graph
;                         :resize resize-graph
;                         :redraw redraw-graph}
;         renderer {:graphics graphics
;                   :canvas canvas
;                   :selected nil
;                   :nearest nil
;                   :cursor nil
;                   :graph-handlers graph-handlers
;                   :nodes {}}]
;     (swap! state merge graph-handlers renderer)
;      ;; Arbor.js expects the render to return the handlers
;     (clj->js graph-handlers)))

; (defn init-graph-view []
;   (let [particle-system (.ParticleSystem js/arbor)
;         graph-attributes (model/set-graph-attributes {:gravity false :repulsion 1000 :dt 0.015 :stiffness 900})]
;     (.parameters particle-system (clj->js {:gravity false :repulsion 1000 :dt 0.015 :stiffness 900}))
;     (swap! state assoc :particle-system particle-system)
;     (set! (.-renderer particle-system) (create-renderer "#graph-canvas"))

;     (let [foo (model/create-node {:name "foo"})
;           bar (model/create-node {:name "bar" :parent foo})
;           biz (model/create-node {:name "biz" :parent foo})
;           boz (model/create-node {:name "boz" :parent foo})
;           baz (model/create-node {:name "baz" :parent foo})
;           bez (model/create-node {:name "bez" :parent foo})])))

; (defn view
;   []
;   [:div.container.graph-container
;     [:div.row
;       [:h3 "Wikipedia graph"]]
;     [:div.row
;       [:canvas {:id "graph-canvas" :width 900 :height 600}]]])

; ;=================================================================================
; ; Dispatch handlers
; ;=================================================================================

; (defn add-node [id data]
;   (.addNode
;     (get-particle-system) (:id data)
;       (clj->js
;         {:color (get-in data [:data :color])
;          :alpha (get-in data [:data :alpha])})))

; (defn add-edge [id {target :target src :src}]
;   (.addEdge
;     (get-particle-system) src target (clj->js {})))

; (dispatch/react-to #{:edge-created} add-edge)
; (dispatch/react-to #{:node-created} add-node)
; ; (dispatch/react-to #{:node-updated} #(log (str "graph-view :node-updated event fired.")))
; ; (dispatch/react-to #{:edge-updated} #(log (str "graph-view :edge-updated event fired.")))

; (dispatch/react-to #{:toggle-grid} #(swap! state assoc :toggle-grid? %2))
; (dispatch/react-to #{:toggle-fps} #(swap! state assoc :toggle-fps? %2))


