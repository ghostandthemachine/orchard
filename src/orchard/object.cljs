(ns orchard.object
  (:refer-clojure :exclude [set! assoc! dissoc! children])
  (:require [crate.core :as crate]
            [clojure.set :as set]
            [orchard.observe :refer [dom-ready-chan]]
            [orchard.util.log :refer (log log-obj)]
            [cljs.core.async :refer [chan >! <! put! timeout close!]]
            [orchard.util.dom :refer [replace-with]]
            [crate.binding :refer [sub-swap! subatom sub-reset! deref?]]))

(def id-counter    (atom 0))
(def instances     (atom (sorted-map)))
(def behaviors     (atom {}))
(def object-defs   (atom {}))
(def tags          (atom {}))

(declare change)


(defn add [obj]
  (swap! object-defs assoc (:type obj) obj))


(defn add-b [obj]
  (swap! behaviors assoc (:name obj) obj))


(defn ->triggers [obj behs]
  (let [listeners (reduce
                   (fn [res be]
                     (merge-with concat
                                 res
                                 (into {}
                                       (for [t (:triggers (@behaviors be))]
                                         [t [be]]))))
                   {}
                   behs)]
    listeners))


(defn ->id [obj]
  (if (deref? obj)
    (::id @obj)
    (::id obj)))


(defn merge! [obj & m]
  (swap! obj #(apply merge % m)))


(defn instances-by-type [type]
  (filter #(= type (:type (deref %))) (vals @instances)))


(defn by-type [type]
  (instances-by-type type))


(defn tags->behaviors [ts]
  (apply concat (map @tags ts)))


(defn raise [obj k & args]
  (let [reactions (map #(-> (% @behaviors) :reaction) (-> @obj :listeners k))]
    (doseq [r reactions
            :when r]
      (apply r obj args))))


(defn update-listeners [obj]
  (let [behs (set (concat (:behaviors obj) (tags->behaviors (:tags obj))))]
    (assoc obj :listeners (->triggers obj behs))))


(defn make-object* [name & r]
  (let [obj (merge {:behaviors #{} :tags #{} :triggers [] :listeners {} :type name :children {}}
                   (apply hash-map r))]
    obj))


(defn store-object* [obj]
  (add obj)
  obj)


(defn handle-redef [odef]
  (let [id (:type odef)]
    (doseq [o (instances-by-type id)
            :let [o (deref o)
                  args (:args o)
                  old  (:content o)
                  behs (set (:behaviors o))
                  inst (@instances (->id o))
                  neue (when (:init odef)
                         (apply (:init odef) inst args))
                  neue (if (vector? neue)
                         (crate/html neue)
                         neue)]]
      (merge! inst (update-listeners {:tags (set/union (:tags o) (:tags odef))
                                      :behaviors (set/union behs (set (:behaviors odef)))
                                      :content neue}))
      (when neue
        (replace-with old neue))
      (raise inst :redef))
    id))


(defn object* [name & r]
  (-> (apply make-object* name r)
      (store-object*)
      (handle-redef)))


(defn defined?
  [obj-name]
  (contains? @object-defs obj-name))


(defn make-behavior* [name & r]
  (let [be (merge {:name name}
                  (apply hash-map r))]
    be))

(defn store-behavior* [beh]
  (add-b beh)
  (:name beh))

; (defn wrap-throttle [beh]
;   (if-let [thr (:throttle beh)]
;     (assoc beh :reaction (throttle thr (:reaction beh)))
;     beh))

; (defn wrap-debounce [beh]
;   (if-let [thr (:debounce beh)]
;     (assoc beh :reaction (debounce thr (:reaction beh)))
;     beh))

(defn behavior* [name & r]
  (-> (apply make-behavior* name r)
      ; (wrap-throttle)
      ; (wrap-debounce)
      (store-behavior*)))

(declare create)

(defn ->sub-objects [parent obj-map]
  (into {}
        (for [[k v] obj-map]
          (if-not (= parent v)
            [k (create v)]
            (throw (js/Error. "Recursive sub-objects are not allowed"))))))

(defn update! [obj & r]
  (swap! obj #(apply update-in % r)))

(defn assoc! [obj & args]
  (swap! obj #(apply assoc % args)))

(defn dissoc! [obj & args]
  (swap! obj #(apply dissoc % args)))

(defn ->inst [o]
  (cond
   (map? o) (@instances (->id o))
   (deref? o) o
   :else (@instances o)))

(defn destroy! [obj]
  (let [inst (->inst obj)]
    (raise inst :destroy)
    (swap! instances dissoc (->id inst))
    (reset! obj nil)))

(defn store-inst [inst]
  (swap! instances assoc (::id @inst) inst)
  inst)

(defn create [obj-name & args]
  (let [obj (if (keyword? obj-name)
              (@object-defs obj-name)
              obj-name)
        id (or (::id obj) (swap! id-counter inc))
        obj (update-listeners obj)
        inst (atom (assoc (dissoc obj :init)
                     ::id id
                     :args args
                     :behaviors (set (:behaviors obj))
                     :tags (set (conj (:tags obj) :object))))
        inst (store-inst inst)
        content (when (:init obj)
                  (apply (:init obj) inst args))
        content (if (vector? content)
                  (crate/html content)
                  content)
        final (merge! inst {:content content
                            :ready-chan (dom-ready-chan content)})]
    (add-watch inst ::change (fn [_ _ _ _]
                               (raise inst :object.change)))
    (raise inst :init)
    inst))

(defn refresh! [obj]
  (reset! obj (update-listeners @obj)))

(defn ->def [obj]
  (@object-defs (:type @obj)))

(defn add-behavior! [obj behavior]
  (let [cur (update-in @obj [:behaviors] conj behavior)]
    (reset! obj (update-listeners cur))))

(defn rem-behavior! [obj behavior]
  (let [cur (update-in @obj [:behaviors] #(remove #{behavior} %))]
    (reset! obj (update-listeners cur))))

(defn ->def [def|name]
  (if (map? def|name)
    def|name
    (@object-defs def|name)))

(defn with-behaviors [obj-name behaviors]
  (let [obj-def (->def obj-name)
        cur (update-in obj-def [:behaviors] #(apply conj % behaviors))]
    (update-listeners cur)))

(defn parent! [p child & [name]]
  (update! p [:children] assoc (or name (::id @child)) child)
  (merge! child {:parent (->id p)}))

(defn parent [child]
  (@instances (:parent @child)))

(defn child [obj k]
  ((:children @obj) k))

(defn children [obj]
  (:children @obj))

(defn ->content [obj]
  (:content @obj))

(defn by-tag [tag]
  (filter #(when-let [ts (:tags (deref %))]
             (ts tag))
          (vals @instances)))

(defn has-tag? [obj tag]
  ((:tags @obj) tag))

(defn add-tags [obj ts]
  (let [cur @obj
        cur (update-in cur [:tags] #(reduce conj % ts))]
    (reset! obj (update-listeners cur))
    (raise obj ::tags-added ts)
    obj))

(defn remove-tags [obj ts]
  (let [cur @obj
        behs (apply concat (map @tags ts))
        cur (-> cur
                (update-in [:tags] #(reduce disj % ts))
                (update-in [:behaviors] #(reduce disj % behs))
                (update-in [:listeners] #(apply dissoc % ts)))]
    (reset! obj (update-listeners cur))
    (raise obj ::tags-removed ts)
    obj))

(defn tag-behaviors [tag behs]
  (swap! tags update-in [tag] #(reduce conj
                                       (or % #{})
                                       behs))
  (doseq [cur (by-tag tag)]
    (refresh! cur))
  (@tags tag))

(defn remove-tag-behaviors [tag behs]
  (swap! tags update-in [tag] #(reduce disj
                                       (or % #{})
                                       behs))
  (doseq [cur (by-tag tag)
          b behs]
    (rem-behavior! cur b)))

(defn on-change [obj func]
  (add-watch obj (gensym change) (fn [_ _ _ v]
                                   (func v))))


(defn object-by-id
	[id]
	(->
		(filter
			#(= (:id @(second %)) id)
			@instances)
		first
		last))
