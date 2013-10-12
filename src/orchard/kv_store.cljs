(ns orchard.kv-store
  (:require-macros
    [cljs.core.async.macros :refer [go alt!]])
  (:require
    [orchard.util.log :refer (log log-obj log-err)]
    [orchard.model :refer (ObjectStore ObjectIndex)]))

(defn app-id
  [id]
  (let [id (if (keyword id) (name id) (str id))]
    (str "orchard/" id)))


;; Local Key-Value Store API

(defn local-set
  "Set a key/value pair in the persistent local storage."
  [k v]
  (log "local-set " k " = " v)
  (let [k (if (keyword? k) (name k) (str k))]
    (aset js/localStorage k (.stringify js/JSON (clj->js v))))
  v)


(defn local-get
  "Get a value in the persistent local storage by key."
  [k]
  ;(log "local-get: " k)
  (try
    (let [k (if (keyword? k) (name k) (str k))
          obj-str (aget js/localStorage k)
          obj (if obj-str
                (js->clj (.parse js/JSON obj-str)
                         :keywordize-keys true)
                nil)]
      obj)
    (catch js/Error e
      (log "Exception in local-get: " e)
      (log "couldn't parse object - key = " k))))


(defn local-remove
  "Delete an item from local storage."
  [k]
  (let [k (if (keyword? k) (name k) (str k))]
    (.removeItem js/localStorage k)))


(defn local-clear
  "Delete all items from local storage."
  []
  (.clear js/localStorage))


(defn local-count
  "Returns the number of items currently in local storage."
  []
  (.-length js/localStorage))


(defn local-key-seq
  "Returns a seq of all the keys for items in local storage."
  []
  (map #(.key js/localStorage %) (range (local-count))))


(defn local-item-seq
  "Returns a seq of (key value) pairs for all items in local storage."
  []
  (map (fn [k] [k (local-get k)]) (local-key-seq)))


(deftype LocalStore
  []
  ObjectStore
  (save-object! [this id value]
      (go (local-set (app-id id) value)))

  (get-object [this id]
    (go (local-get (app-id id))))

  (all-objects [this]
    (go (map second (local-item-seq))))

  ObjectIndex
  (objects-of-type [this obj-type]
    (go (map second (filter (fn [[k v]] (= (name obj-type) (:type v))) (local-item-seq))))))

;ObjectIndex
;  (modules-by-type [this m-type]
;    (js/Object.keys (local-get (str "orchard/type-index/" m-type)))))
;(when-let [t (:type value)]
;      (local-set (str "orchard/type-index/" t)
;                    (conj (local-get (str "orchard/type-index/" t)) value)))

(defn local-store
  []
  (LocalStore.))

