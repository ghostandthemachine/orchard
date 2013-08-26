(ns orchard.sql
  "SQL Database API."
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :refer [chan >! <! put! timeout close!]]
    [orchard.util.log :refer (log log-obj)]))


(def DEFAULT-SQL-DB-SIZE (* 1024 1024))


(defn db
  "Returns a named, local WebSQL database."
  ([db-name] (db db-name "1.0" db-name DEFAULT-SQL-DB-SIZE))
  ([db-name version description size]
   (js/openDatabase db-name version description size)))


(defn run
  "Execute a SQL statement on the db."
  [db stmt]
  (let [res-chan (chan)]
    (go
      (.transaction db
        (fn [tx]
          (.executeSql tx stmt (clj->js [])
                       (fn [tx results]
                         (>! res-chan results))))))
      res-chan))


