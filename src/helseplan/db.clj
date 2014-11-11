(ns helseplan.db
  (:require [clojure.java.jdbc :as jdbc]))


(defn hent-medlem [ds id]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (first (jdbc/query conn ["SELECT * FROM medlem where id = ?" id]))))

(defn nytt-medlem [ds medlem]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (let [res (jdbc/insert! conn :medlem medlem)]
      ((keyword "scope_identity()") (first res)))))

(defn oppdater-medlem [ds medlem]
  (let [id (:id medlem)
        felter (dissoc medlem :id)]
    (jdbc/with-db-connection [conn {:datasource ds}]
      (jdbc/update! conn :medlem felter ["id = ?" id]))))

(defn list-medlemmer [ds]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (jdbc/query conn ["SELECT * FROM medlem"])))
