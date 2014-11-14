(defproject helseplan-clojure "0.0.1"
  :description "Helseplan the Clojure way"
  :url "https://github.com/animalia/helseplan-clojure"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.h2database/h2 "1.3.170"]
                 [hikari-cp "0.9.1"]
                 [compojure "1.2.1"]
                 [liberator "0.12.2"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [selmer "0.7.2"]
                 [com.cemerick/friend "0.2.1"]]
  :ring {:handler helseplan.web/app
         :init helseplan.web/bootstrap} ;; Dev spesifikt burde vel i egen profil
  :profiles {:dev {:plugins [[lein-ring "0.8.13"]]
                   :source-paths ["dev"]
                   :test-paths ^:replace []}
             :test {:dependencies [[midje "1.6.3"]]
                    :plugins [[lein-midje "3.1.3"]]
                    :test-paths ["test"]
                    :resource-paths ["test/resources"]}})
