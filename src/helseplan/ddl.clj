(ns helseplan.ddl
  (:require [clojure.java.jdbc :as jdbc]))


(defn create-schema! [ds]
  (jdbc/with-db-connection [conn {:datasource ds}]

    (jdbc/db-do-commands conn
                         (jdbc/execute! conn ["drop all objects"])
                         (jdbc/create-table-ddl :medlem
                                                [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
                                                [:fornavn "varchar(100) NOT NULL"]
                                                [:etternavn "varchar(100) NOT NULL"]
                                                [:prodnr "varchar(10) NOT NULL"])
                         (jdbc/create-table-ddl :veterinaer
                                                [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
                                                [:fornavn "varchar(100) NOT NULL"]
                                                [:etternavn "varchar(100) NOT NULL"]
                                                [:veterinaernr "varchar(4) NOT NULL"])
                         (jdbc/create-table-ddl :helseplan
                                                [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
                                                [:medlem_id "INTEGER NOT NULL"]
                                                [:veterinaer_id "INTEGER NOT NULL"]
                                                [:navn "varchar(100) NOT NULL"]
                                                [:dato "date"]))))

(defn- create-medlem! [conn medlem]
  (jdbc/insert! conn :medlem medlem))

(defn- create-veterinaer! [conn veterinaer]
  (jdbc/insert! conn :veterinaer veterinaer))

(defn- create-helseplan! [conn helseplan]
  (jdbc/insert! conn :helseplan helseplan))


(defn create-sample-data! [ds]
  (jdbc/with-db-connection [conn {:datasource ds}]
    (create-medlem! conn {:id 1 :fornavn "Magnus" :etternavn "Rundberget" :prodnr "0123456789"})
    (create-veterinaer! conn {:id 1 :fornavn "Dr" :etternavn "Dyregod" :veterinaernr "1234"})
    (create-helseplan! conn {:id 1 :medlem_id 1 :veterinaer_id 1 :navn "Magz Plan"})
    ))

