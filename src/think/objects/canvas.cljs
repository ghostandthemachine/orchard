(ns think.objects.canvas
  (:refer-clojure :exclude [rem reset!])
  (:require [think.object :as object]
            [think.objects.context :as ctx]
            [think.util.dom :refer [$ html parent toggle-class append remove prevent stop-propagation css] :as dom])
  (:use [crate.binding :only [bound]])
  (:use-macros [crate.def-macros :only [defpartial]]
               [think.macros :only [defui]]))

(declare position!)
(declare ->rep)
(declare get-rep)
(declare rem!)



(object/behavior* ::refresh
                  :triggers #{:refresh}
                  :reaction (fn [obj]
                              (js/window.location.reload true)))

(object/behavior* ::remove-on-destroy
                  :triggers #{:destroy}
                  :reaction (fn [obj]
                              (rem! obj)))

(object/behavior* ::rep-on-redef
                  :triggers #{:redef}
                  :reaction (fn [obj]
                              (->rep obj)))

(object/behavior* ::alt-down-drag
                  :triggers #{:object.mousedown}
                  :reaction (fn [canv obj e]
                              (when (.-altKey e)
                                (object/merge! canv {:dragging obj})
                                (dom/prevent e)
                                (dom/stop-propagation e))))

(object/behavior* ::dragging
                  :triggers #{:mousemove}
                  :reaction (fn [canv e]
                              (when-let [obj (:dragging @canv)]
                                (let [$drag (get-rep obj)]
                                  (dom/css $drag {:position "absolute"
                                              :margin 0
                                              :z-index 1})
                                  (comment
                                    (object/merge! obj {::position {:top (- (.-pageY e) (/ (dom/height $drag) 2))
                                                                    :left (-  (.-pageX e) (/ (dom/width $drag) 2))}}
                                                   ))
                                  (position! obj {:top (- (.-pageY e) (/ (dom/height $drag) 2))
                                                                  :left (-  (.-pageX e) (/ (dom/width $drag) 2))})))))
(object/behavior* ::drag-end
                  :triggers #{:mouseup}
                  :reaction (fn [canv e]
                              (when-let [obj (:dragging @canv)]
                                (let [$drag (get-rep obj)]
                                  (css $drag {:z-index 0})
                                  (object/merge! canv {:dragging nil})))))

(defui canvas-elem [obj]
  [:div#canvas]
  :mousemove (fn [e]
               (object/raise obj :mousemove e))
  :click (fn [e]
           (object/raise obj :click e))
  :mousedown (fn [e] (object/raise obj :mousedown e))
  :mouseup (fn [e] (object/raise obj :mouseup e))
  :contextmenu (fn [e] (object/raise obj :contextmenu e)))

(defn ->rep [obj canvas]
  (let [content (:content @obj)]
    (dom/attr content {:objId (object/->id obj)})
    (comment
    (dom/on* content {:mouseup (fn [e]
                                 (object/raise canvas :object.mouseup obj e))
                      :mousemove  (fn [e]
                                   (object/raise canvas :object.mousemove obj e))
                      :mousedown (fn [e]
                                   (object/raise canvas :object.mousedown obj e))
                      :contextmenu (fn [e]
                                     (object/raise canvas :object.contextmenu obj e))
                      :click (fn [e]
                               (object/raise canvas :object.click obj e))}))
    content))



(object/object* ::canvas
                :triggers [:mousemove :mousedown :mouseup :contextmenu :click
                           :object.mousemove :object.mousedown :object.mouseup
                           :object.click :object.contextmenu]
                :behaviors [::alt-down-drag ::dragging ::drag-end]
                :init (fn [obj]
                        (canvas-elem obj)))

(def canvas (object/create ::canvas))

(defn add! [obj & [position?]]
  (object/add-behavior! obj ::remove-on-destroy)
  (object/add-behavior! obj ::rep-on-redef)
  (let [rep (->rep obj canvas)]
    (dom/append (:content @canvas) rep)
    (object/raise obj :show rep)
    (when position?
      (object/merge! obj {::position {:top 50 :right 10}})
      (position! obj {:top 50 :right 10}))))


(defn get-rep [obj]
  ($ (str "[objid='" (object/->id obj) "']") (:content @canvas)))

(defn position! [obj pos]
  (css (get-rep obj) (merge {:position "absolute"
                             :left "auto"
                             :right "auto"
                             :bottom "auto"
                             :top "auto"} pos)))

(defn rem! [obj]
  (when-let [rep (get-rep obj)]
    (dom/remove rep))
  (object/raise obj :object.remove))


(defn replace! [obj1 obj2]
  (rem! obj1)
  (add! canvas obj2))

(defn reset!
  [obj1 obj2]
  (dom/empty obj1)
  (add! canvas obj2))

(defn ->px [s]
  (str (or s 0) "px"))


(dom/append (dom/$ :#wrapper) (:content @canvas))


(ctx/in! :global canvas)
