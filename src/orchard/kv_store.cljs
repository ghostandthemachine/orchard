(ns orchard.kv-store
  (:require 
    [orchard.util.log :refer (log log-obj log-err)]))

;; Local Key-Value Store API

(defn local-set
  "Set a key/value pair in the persistent local storage."
  [k v]
  (let [k (if (keyword? k) (name k) (str k))]
    (aset js/localStorage k (.stringify js/JSON (clj->js v))))
  v)


(defn local-get
  "Get a value in the persistent local storage by key."
  [k]
  (try
    (log "local-get: " k)
    (let [k (if (keyword? k) (name k) (str k))
          obj-str (aget js/localStorage k)
          obj (.parse js/JSON obj-str)
          clj-obj (js->clj obj :keywordize-keys true)]
      clj-obj)
    (catch js/Error e
      (log "Exception in local-get")
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
