(defproject helseplan-clojure "0.0.1"
  :description "Helseplan the Clojure way"
  :url "https://github.com/animalia/helseplan-clojure"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [com.h2database/h2 "1.3.170"]
                 [compojure "1.2.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [selmer "0.7.2"]]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler helseplan.web/routes})
