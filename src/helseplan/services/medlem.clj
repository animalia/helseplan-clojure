(ns helseplan.services.medlem
  (:require [clojure.java.jdbc :as jdbc]))


(defn hent [ds id]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (first (jdbc/query conn ["SELECT * FROM medlem where id = ?" id]))))

(defn ny [ds medlem]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (let [res (jdbc/insert! conn :medlem medlem)]
      ((keyword "scope_identity()") (first res)))))

(defn oppdater [ds medlem]
  (let [id (:id medlem)
        felter (dissoc medlem :id)]
    (jdbc/with-db-connection [conn {:datasource ds}]
      (jdbc/update! conn :medlem felter ["id = ?" id]))))

(defn slett [ds id]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (jdbc/delete! conn :medlem ["id = ?" id])))

(defn finn [ds]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (jdbc/query conn ["SELECT * FROM medlem"])))
