(ns think.sql
  "SQL Database API."
  (:use-macros [redlobster.macros :only [when-realised defer-node let-realised]])
  (:require [redlobster.promise :as p]
            [think.util.core :as util]
            [think.util.log :refer (log log-obj log-err)]))



(def DEFAULT-SQL-DB-SIZE (* 1024 1024))


(defn db
  "Returns a named, local WebSQL database."
  ([db-name] (db db-name "1.0" db-name DEFAULT-SQL-DB-SIZE))
  ([db-name version description size]
   (js/openDatabase db-name version description size)))


(defn run
  "Execute a SQL statement on the db."
  [db stmt]
  (let [res-promise (p/promise)]
    (.transaction db
      (fn [tx]
        (.executeSql tx stmt (clj->js [])
          (fn [tx results]
            (p/realise res-promise results)))))
    res-promise))


