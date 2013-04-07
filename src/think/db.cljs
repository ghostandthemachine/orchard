(ns think.db
  (:use [think.log :only (log log-obj log-err)])
  (:use-macros [redlobster.macros :only [promise]])
  (:require-macros [think.macros :as mac])
  (:require [redlobster.promise :as p]))

(def ^:private pouch   (js/require "pouchdb"))
(def db* (atom nil))

(defn handle-error
  [err]
  (log-obj err))

(defn handle-realised
  ([p err resp]
    (handle-realised p err resp nil))
  ([p err resp err-handler]
  (if err
    (when err-handler
      (err-handler err))
    (p/realise p resp))))

(defn attach-realise-handler
  [db p f1 f2]
  (p/on-realised p
    (if f1
      #(f1 db %)
      #())
    (if f2
      #(f2 %)
      #(log-err ""))))

(defn db-open
  [path]
  (let [pouch-promise (p/promise)]
    (pouch (str "leveldb://" path) (cljs.core/clj->js {}) (partial handle-realised pouch-promise))
    pouch-promise))

(defn db-get
  ([db id] (db-get db id nil))
  ([db id f] (db-get db id f nil))
  ([db id f1 f2]
  (let [get-promise (p/promise)]
    (.get db (clj->js id) (partial handle-realised get-promise))
    (attach-realise-handler db get-promise f1 f2)
    get-promise)))

(defn db-put
  ([db doc] (db-put db doc nil))
  ([db doc f] (db-put db doc f nil))
  ([db doc f1 f2]
   "Insert or modify a document, which must have a key \"_id\" or :_id."
  (let [put-promise (p/promise)]
    (.put db (clj->js doc) (partial handle-realised put-promise))
    (attach-realise-handler db put-promise f1 f2)
    put-promise)))

(defn db-post
  ([db doc] (db-post db doc nil))
  ([db doc f] (db-post db doc f nil))
  ([db doc f1 f2]
   "Insert or modify a document. Respoinse will contain the generated key."
  (let [post-promise (p/promise)]
    (.post db doc (partial handle-realised post-promise))
    (attach-realise-handler db post-promise f1 f2)
    post-promise)))


(defn db-remove
  ([db doc] (db-remove db doc nil))
  ([db doc f] (db-remove db doc f nil))
  ([db doc f1 f2]
  (let [remove-promise (p/promise)]
    (.remove db (clj->js doc) (partial handle-realised remove-promise))
    (attach-realise-handler db remove-promise f1 f2)
    remove-promise)))


(defn db-info
  ([db f] (db-remove db f nil))
  ([db f1 f2]
  (let [info-promise (p/promise)]
    (.info db (partial handle-realised info-promise))
    (attach-realise-handler db info-promise f1 f2)
    info-promise)))


(defn db-replicate
  "Replicate source to target. Source and target can be either local DB names
  or remote locations, i.e. URLs."
  ([src tgt] (db-replicate src tgt nil))
  ([src tgt f] (db-replicate src tgt f nil))
  ([src tgt f1 f2]
  (let [replicate-promise (p/promise)]
    (.replicate js/Pouch src tgt (clj->js {}) (partial handle-realised replicate-promise))
    (p/on-realised replicate-promise
      #(f2 %)
      #(f1 %))
    replicate-promise)))
