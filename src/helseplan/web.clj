(ns helseplan.web
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :as ring]
            [selmer.parser :refer [render-file]]))

(defroutes routes
  (GET "/" [] "<h2>Hello World</h2>")
  (GET "/main" [] (render-file "templates/main.html" {:header "Helseplan Main"})))

(defn -main [& [port]]
  (ring/run-jetty #'routes {:port 8080 :join? false}))
