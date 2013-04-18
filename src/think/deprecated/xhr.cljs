(ns think.xhr
  (:require [goog.net.XhrIo :as xhr]
            [clojure.string :as string]
            [goog.events :as events]
            [goog.Uri.QueryData :as query-data]
            [goog.structs :as structs]
            [think.util :refer [log]]
            [redlobster.promise :as p]))

(defn ->method [m]
  (string/upper-case (name m)))

(defn parse-route [route]
  (cond
    (string? route) ["GET" route]
    (vector? route) (let [[m u] route]
                      [(->method m) u])
    :else ["GET" route]))

(defn ->data [d]
  (let [cur (clj->js d)
        query (query-data/createFromMap (structs/Map. cur))]
    (str query)))

(defn ->callback [callback]
  (when callback
    (fn [req]
      (let [data (. req (getResponseText))]
        (callback data)))))

(defn xhr [route content callback & [opts]]
  (let [req (new goog.net.XhrIo)
        [method uri] (parse-route route)
        data (->data content)
        callback (->callback callback)]
    (when callback
      (events/listen req goog.net.EventType/COMPLETE #(callback req)))
    (.send req uri method data (when opts (clj->js opts)))))

(defn xhr-promise [route content & [opts]]
  (let [xhr-p (p/promise)
        realise-callback (fn [req] (p/realise xhr-p req))]
    (xhr route content realise-callback opts)
    xhr-p))
