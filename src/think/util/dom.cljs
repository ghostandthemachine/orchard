(ns think.util.dom
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:refer-clojure :exclude [parents remove next val empty])
  (:require
    [cljs.core.async :refer [chan >! <! timeout]]))


(defn has?
  [coll k]
  (not (nil? (some #{k} coll))))


(defn- lazy-nl-via-item
  ([nl] (lazy-nl-via-item nl 0))
  ([nl n] (when (< n (. nl -length))
            (lazy-seq
             (cons (. nl (item n))
                   (lazy-nl-via-item nl (inc n)))))))


(extend-type js/HTMLCollection
  IIndexed
  (-nth [this n]
    (.item this n))
  (-nth [this n not-found]
        (or (.item this n) not-found))

  ISeqable
  (-seq [this] (lazy-nl-via-item this))

  ICounted
  (-count [this] (.-length this)))


(extend-type js/NodeList
  ISeqable
  (-seq [this] (lazy-nl-via-item this))

  ICounted
  (-count [this] (.-length this))

  IIndexed
  (-nth [this n]
    (.item this n))
  (-nth [this n not-found]
        (or (.item this n) not-found)))

;; CSS selection, modification

(defn $$
  ([query]
    ($$ query nil))
  ([query elem]
  (let [elem (or elem js/document)
        res (.querySelectorAll elem (name query))]
    res)))


(defn $
  ([query] ($ query nil))
  ([query elem]
  (let [elem (or elem js/document)
        res (.querySelector elem (name query))]
    res)))


(defn by-id
  [id]
  (js/$ (str "#" id)))


(defn by-tag
  [tag]
  (js->clj (js/document.getElementsByTagName (name tag))))


(defn by-class
  [class]
  (js/$ (str "." class)))


(defn append [parent child]
  (.appendChild parent child)
  parent)


(defn add-class [elem class]
  (when elem
    (.add (.-classList elem) (name class))))


(defn add-id
  [elem id]
  (when elem
    (aset elem "id" (name id))))

(defn remove-class [elem class]
  (when elem
    (.remove (.-classList elem) (name class))))

(defn has-class? [elem class]
  (when elem
    (.contains (.-classList elem) (name class))))

(defn toggle-class [elem class]
  (if (has-class? elem class)
    (remove-class elem class)
    (add-class elem class)))

(defn set-css [elem things]
  (doseq [[k v] things]
    (aset (.-style elem) (name k) (if (keyword? v) (name v) v))))

(defn css [elem things]
  (let [things (if (= js/Object (type things))
            (js->clj things)
            things)]
    (if (map? things)
      (set-css elem things)
      (aget (.-style elem) (name things)))))

(defn set-attr [elem things]
  (doseq [[k v] things]
    (.setAttribute elem (name k) (if (keyword? v) (name v) v))))

(defn attr [elem things]
  (if (map? things)
    (set-attr elem things)
    (.getAttribute elem (name things))))

(defn parent [elem]
  (.-parentNode elem))

(defn children [elem]
  (.-children elem))

(defn remove [elem]
  (.removeChild (parent elem) elem))

(defn empty [elem]
  (while (seq (.-children elem))
    (.removeChild elem (aget (.-children elem) 0))))

(defn val [elem & [v]]
  (if-not v
    (.-value elem)
    (set! (.-value elem) v)))

(defn siblings [elem]
  (.-children (parent elem)))

(defn parents [elem sel]
  (let [root (parent ($ :body js/document))]
  (loop [p (parent elem)]
    (when-not (= p root)
      (if (.webkitMatchesSelector p (name sel))
        p
        (recur (parent p)))))))

(defn next [elem]
  (.-nextElementSibling elem))

(defn before [elem neue]
  (.insertBefore (parent elem) neue elem))

(defn after [elem neue]
  (before (next elem) neue))

(defn replace-with [orig neue]
  (when-let [p (parent orig)]
    (.replaceChild p neue orig)))

(defn height [elem]
  (.-clientHeight elem))

(defn width [elem]
  (.-clientWidth elem))

(defn offset-top [elem]
  (.-offsetTop elem))

(defn scroll-top [elem & [v]]
  (if-not v
    (.-scrollTop elem)
    (set! (.-scrollTop elem) v)))

(defn top [elem]
  (css elem :top))

(defn bottom [elem]
  (css elem :bottom))

(defn left [elem]
  (css elem :left))

(defn right [elem]
  (css elem :right))

(defn html [elem & [h]]
  (if-not h
    (.-innerHTML elem)
    (set! (.-innerHTML elem) h)))


(defn ->event
  [ev]
  (str (name ev)))


(defn make [str]
  (let [d (.createElement js/document "div")]
    (html d str)
    (children d)))


(defn index [e]
  (let [p (parent e)
        c (children p)
        len (.-length c)]
    (loop [i 0]
      (if (>= i len)
        nil
        (if (= (aget c i) e)
          i
          (recur (inc i)))))))


(defn module-element
  [module]
  ($ (str "#module-" (:id @module))))

(defn scroll-to-end
  [scrollable-elem]
  (set! (.-scrollTop scrollable-elem)
    (.-scrollHeight scrollable-elem)))

;; Events

(defn prevent
  "Prevent default event behavior after this handler."
  [e]
  (.preventDefault e))


(defn stop-propagation
  "Stop event propagation."
  [e]
  (.stopPropagation e))


(defn trigger
  "Trigger an event on a dom element."
  [elem ev]
  (let [e (.createEvent js/document "HTMLEvents")]
    (.initEvent e (name ev) true true)
    (.dispatchEvent elem e)))


(defn on-event
  "Add an event handler to a dom element."
  [elem event cb]
  (.addEventListener elem (->event event) cb))


(defn clear-event
  "Remove an event handler for a dom element."
  [elem ev cb]
  (.removeEventListener elem (->event ev) cb))


(defn on-event*
  [elem evs]
  (doseq [[ev cb] evs]
    (.addEventListener elem (->event ev) cb)))


(defn active-element []
  (.-activeElement js/document))


(defn focus [elem]
  (.focus elem))


(defn on-doc-ready [func]
  (on-event js/document :DOMContentLoaded func))


(defn window-width
  []
  (aget js/window "innerWidth"))


(defn set-frame-title
  [title]
  (aset js/document "title" title))


(defn data-attr
  "Get data attribute values and convert them to JSON."
  [elem k]
  (let [ks      (if (seq? k)
                  k
                  [k])
        dataset (.-dataset elem)
        values  (js->clj
                  (reduce
                    (fn [values k]
                      (conj values (aget dataset (name k))))
                    []
                    ks))]
    (->
      (if (= (count values) 1)
        (first values)
        values)
      js/JSON.parse
      js->clj)))

(defn set-data-attr!
  "Set data attribute values.
  Currently does not suuport keyword values (they are converted strings)."
  [elem & opts]
  (let [dataset (.-dataset elem)]
    (doseq [[k v] (partition 2 (flatten opts))]
      (aset dataset (name k) (js/JSON.stringify (clj->js v))))
    (js->clj dataset)))


(defn element
  "Creates an HTML element. Optionally takes a list of attributes"
  [s]
  (.createElement js/document (name s)))


(defn observer-chan

  "Returns a [dom-observer channel] tuple.  The channel will receive a stream
  of dom mutation events, and the dom-observer can be used to stop observing.
  The supported options to request event types are :attrs, :children, and :text.

  (let [[obs obs-chan] (observer-chan $(:#content) :children)]
    (go
      (log-obj (<! obs-chan))
      (stop! obs)))
  "
  [target & options]
  (let [c (chan 10)
        opts (reduce #(assoc %1 %2 true) {} options)
        opts (assoc opts "attributes"    (:attrs opts))
        opts (assoc opts "childList"     (:children opts))
        opts (assoc opts "characterData" (:text opts))]
      (let [handler (fn [mutations]
                      (go
                        (doseq [mutation mutations]
                          (>! mutation c))))
            observer (js/MutationObserver.   handler)
            config   (clj->js opts)]
        (.observe observer target config)
        [observer c])))


(defn stop-observer!
  "Stop a dom observer."
  [observer]
  (.disconnect observer))


;(defn handle-file-select [evt]
;(.stopPropagation evt)
;(.preventDefault evt)
;(let [files (.-files (.-dataTransfer evt))]
;(dotimes [i (.-length files)]
;(let [rdr (js/FileReader.)
;the-file (aget files i)]
;(set! (.-onload rdr)
;(fn [e]
;(let [file-content (.-result (.-target e))
;file-name (if (= ";;; " (.substr file-content 0 4))
;(let [idx (.indexOf file-content "\n\n")]
;(.log js/console idx)
;(.slice file-content 4 idx))
;(.-name the-file))]
;(.log js/console (str "file-name " file-name))
;(.set storage file-name file-content)
;(swap! list-of-code conj file-name))))
;(.readAsText rdr the-file)))))
;
;(defn handle-drag-over [evt]
;(.stopPropagation evt)
;(.preventDefault evt)
;(set! (.-dropEffect (.-dataTransfer evt)) "copy"))
;
;(defn set-up-drop-zone [id]
;(let [body (aget (.getElementsByTagName js/document "body") 0)
;zone (.createElement js/document "div")]
;(when-let [x (.getElementById js/document id)]
;(.removeChild body x))
;(set! (.-innerHTML zone) "Drop Here")
;(.setAttribute zone "id" id)
;(set! (.-position (.-style zone)) "absolute")
;(set! (.-top (.-style zone)) "0px")
;(set! (.-right (.-style zone)) "0px")
;(.appendChild body zone)
;(.addEventListener zone "dragover" handle-drag-over false)
;(.addEventListener zone "drop" handle-file-select false)))
