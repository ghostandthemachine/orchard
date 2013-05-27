(ns think.kv-store)

;; Local Key-Value Store API

(defn local-set
  "Set a key/value pair in the persistent local storage."
  [k v]
  (aset js/localStorage k v)
  v)


(defn local-get
  "Get a value in the persistent local storage by key."
  [k]
  (aget js/localStorage k))

