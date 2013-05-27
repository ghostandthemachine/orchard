(ns think.sql
  (:use-macros [redlobster.macros :only [when-realised defer-node let-realised]])
  (:require [redlobster.promise :as p]
            [think.util :as util]
            [think.util.log :refer (log log-obj log-err)]))

;; SQL Database API

(def DEFAULT-SQL-DB-SIZE (* 1024 1024))

(defn sql-store
  "Returns a named, local WebSQL database."
  ([db-name] (sql-store db-name "1.0" db-name DEFAULT-SQL-DB-SIZE))
  ([db-name version description size]
   (js/openDatabase db-name version description size)))

(defn sql-exec
  "Execute a SQL statement on the db."
  [db stmt]
  (let [res-promise (p/promise)]
    (.transaction db
      (fn [tx]
        (.executeSql tx stmt (clj->js [])
          (fn [tx results]
            (p/realise res-promise results)))))
    res-promise))


