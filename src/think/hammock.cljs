(ns think.hammock
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! put! timeout close! get!]]
    [think.objects.logger :as logger]
    [think.util.core :as util]
    [think.util.fetch :refer (xhr)]
    [think.util.log :refer (log log-obj log-err)]))


(defn base-url
  [r]
  (str "http://127.0.0.1:5984/" r))


(defn log-error
  [err msg]
  (log "CouchDB Error: " err " " msg))


(defn encode-json
  [content]
  (let [f #(if (= (type %2) js/Function)
             (.toString %2)
             %2)]
    (try
      (.stringify js/JSON content f)
      (catch js/Error e
        (log-error e "Bad JSON content passed")))))


(defn hammock
  "Base function for hammock, a purely channel based couchdb client.

  Takes a map of args.

  ex: (hammock
        :method  :put
        :db-arg \"test-db\"))

  returns a channel that will recieve data on creating a new database."
  [& args]
  (let [args-map (apply hash-map args)
        data (merge
              {:method   :get
               :db-arg  "_all_dbs"}
              args-map)
        {:keys [method db-arg]} data
        method (clojure.string/upper-case (name method))
        res-chan  (chan)]
    (if (:content args-map)
      (let [content     (:content args-map)
            document-id (or (:id content) (:_id content) ("id" content) ("_id" content))
            data        (if (.isBuffer js/Buffer content)
                          content
                          (encode-json (clj->js content)))
            url         (str (base-url db-arg) "/" document-id)]
        (think.util.fetch/xhr [method url] data #(put! res-chan %)))
      (think.util.fetch/xhr [method (base-url db-arg)] {} #(put! res-chan %)))
    res-chan))



(defn all-dbs
  []
  (hammock
    :db-arg "_all_dbs"))

; (go (log (<! (all-dbs))))


(defn create-db
  [db-name]
  (hammock
    :method :put
    :db-arg    db-name))

; (go (log (<! (create-db "testing-db-create"))))


(defn delete-db
  [db-name]
  (hammock
    :method :delete
    :db-arg    db-name))

; (go (log (<! (delete-db "testing-db-create"))))



;; not working yet. Need to fix invalid json error...
(defn insert-document
  [db-name document]
  (hammock
    :method   :put
    :content  document
    :db-arg   db-name))

; (go (log (<! (insert-document "test-db" {:foo "bar" :wiz "woz"}))))