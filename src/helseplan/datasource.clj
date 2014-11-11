(ns helseplan.datasource
  (:require [hikari-cp.core :refer :all]))

(def config {:url "jdbc:h2:mem"
             :adapter :h2})

(def ds-config (datasource-config config))

(defn get-ds []
  (defonce datasource (datasource-from-config ds-config))
  datasource)
