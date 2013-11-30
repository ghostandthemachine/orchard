(ns orchard.hammock
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! put! timeout close!]]
    [orchard.objects.logger :as logger]
    [orchard.util.core :as util]
    [orchard.util.fetch :refer (xhr)]
    [orchard.util.log :refer (log log-obj log-err)]))

(defn couch-ids
  [x]
  (let [id  (:id x)
        rev (:rev x)
        x   (dissoc x :id :rev)]
      (if (and id rev)
        (assoc x "_id" id "_rev" rev)
        (assoc x "_id" id))))


(defn cljs-ids
  [x]
  (let [id   (or
              (:_id x)
              (:id x))
        rev  (or
              (:_rev x)
              (:rev x))
        type (:type x)
        x    (dissoc x :_id :_rev)]
    (assoc x :id id :rev rev :type type)))


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
  "Base function for hammock, a channel based couchdb client.

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
            document-id (:id content)
            data        (if (.isBuffer js/Buffer content)
                          content
                          (encode-json (-> content couch-ids clj->js)))
            url         (str (base-url db-arg) "/" document-id)]
            (log-obj data)
            (log-obj (.isBuffer js/Buffer content))
        (orchard.util.fetch/xhr [method url] data #(put! res-chan %)))
      (orchard.util.fetch/xhr [method (base-url db-arg)] {} #(put! res-chan %)))
    res-chan))



(defn all-dbs
  []
  (hammock
    :db-arg "_all_dbs"))

; (go (log (<! (all-dbs))))


(defn create-db
  [db-name]
  (hammock
    :method   :put
    :db-arg   db-name))

; (go (log (<! (create-db "testing-db-create"))))


(defn delete-db
  [db-name]
  (hammock
    :method   :delete
    :db-arg   db-name))

; (go (log (<! (delete-db "testing-db-create"))))



;; not working yet. Need to fix invalid json error...
(defn insert-document
  "Insert a document into a given database. Doc must contain an id."
  [db-name document]
  (hammock
    :method   :put
    :content  document
    :db-arg   db-name))

; (go (log (<! (insert-document "test-db" {:id 1 :foo "bar" :wiz "woz"}))))
; (defn couch-ids
;   [x]
;   (let [id  (:id x)
;         rev (:rev x)
;         x   (dissoc x :id :rev)]
;       (if (and id rev)
;         (assoc x "_id" id "_rev" rev)
;         (assoc x "_id" id))))


; (defn cljs-ids
;   [x]
;   (let [id   (or
;               (:_id x)
;               (:id x))
;         rev  (or
;               (:_rev x)
;               (:rev x))
;         type (:type x)
;         x    (dissoc x :_id :_rev)]
;     (assoc x :id id :rev rev :type type)))


; (defn base-url
;   [r]
;   (str "http://127.0.0.1:5984/" r))


; (defn log-error
;   [err msg]
;   (log "CouchDB Error: " err " " msg))


; (defn encode-json
;   [content]
;   (let [f #(if (= (type %2) js/Function)
;              (.toString %2)
;              %2)]
;     (try
;       (.stringify js/JSON content f)
;       (catch js/Error e
;         (log-error e "Bad JSON content passed")))))


; (defn hammock
;   "Base function for hammock, a channel based couchdb client.

;   Takes a map of args.

;   ex: (hammock
;         :method  :put
;         :db-arg \"test-db\"))

;   returns a channel that will recieve data on creating a new database."
;   [& args]
;   (let [args-map (apply hash-map args)
;         data (merge
;               {:method   :get
;                :db-arg  "_all_dbs"}
;               args-map)
;         {:keys [method db-arg]} data
;         method (clojure.string/upper-case (name method))
;         res-chan  (chan)]
;     (if (:content args-map)
;       (let [content     (:content args-map)
;             document-id (:id content)
;             data        (if (.isBuffer js/Buffer content)
;                           content
;                           (encode-json (-> content couch-ids clj->js)))
;             url         (str (base-url db-arg) "/" document-id)]
;             (log-obj data)
;             (log-obj (.isBuffer js/Buffer content))
;         (think.util.fetch/xhr [method url] data #(put! res-chan %)))
;       (think.util.fetch/xhr [method (base-url db-arg)] {} #(put! res-chan %)))
;     res-chan))



; (defn hammock
;   [opts]
;   (let [;; if opts is a string we just make a map of it for a simple GET request
;         opts (if (= (type opts) js/String)
;               {:opts opts}
;               opts)
;         params (or (:params opts) {})
;         headers {"content-type" "application/json"
;                  "accept"       "application/json"}
;         req     (atom
;                   {:method  (or (:method opts) :get)
;                    :headers headers
;                    :uri     "http://127.0.0.1:5984"})
;         update-req (fn [k f] (swap! req update-in [k] f))
;         add-to-uri (fn [s] (update-req :uri #(str % (js/encodeURIComponent s))))]

;     ;; add headers if given
;     (when (:headers opts)
;       (update-req :headers (fn [_] (:headers opts))))


;     ;; Begin building request uri

;     ;; add db if given
;     (when (:db opts)
;       (add-to-uri (str "/" (:db opts))))

;     ;; add path if given
;     (when (:path opts)
;       (add-to-uri (str "/" (:path opts))))

;     (cond
;       ;; not a design docment
;       (:doc opts)
;         (if (re-find #"_design" (:doc opts))
;           ;; don't encode design documents
;           (add-to-uri (str "/" (:doc opts)))
;           (try
;             (add-to-uri (str "/" (:doc opts)))
;             (catch js/Error e
;               (log-error "couldn't encode: " (:doc opts) " as a uri")))))))


; (defn all-dbs
;   []
;   (hammock
;     :db-arg "_all_dbs"))

; ; (go (log (<! (all-dbs))))


; (defn create-db
;   [db-name]
;   (hammock
;     :method   :put
;     :db-arg   db-name))

; ; (go (log (<! (create-db "testing-db-create"))))


; (defn delete-db
;   [db-name]
;   (hammock
;     :method   :delete
;     :db-arg   db-name))

; ; (go (log (<! (delete-db "testing-db-create"))))



; ;; not working yet. Need to fix invalid json error...
; (defn insert-document
;   "Insert a document into a given database. Doc must contain an id."
;   [db-name document]
;   (hammock
;     :method   :put
;     :content  document
;     :db-arg   db-name))

; ; (go (log (<! (insert-document "test-db" {:id 1 :foo "bar" :wiz "woz"}))))

; (defn get-document
;   [db-name document-id]
;   (hammock
;     :method :get
;     :content (str document-id)
;     :db-arg db-name))
