(ns orchard.kv-store)

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
  (let [k (if (keyword? k) (name k) (str k))]
    (js->clj (.parse js/JSON (aget js/localStorage k)) :keywordize-keys true)))

