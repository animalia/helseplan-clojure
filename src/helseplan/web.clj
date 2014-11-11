(ns helseplan.web
  (:require [compojure.core :refer [defroutes GET ANY]]
            [ring.adapter.jetty :as ring]
            [liberator.core :refer [resource defresource]]
            [selmer.parser :refer [render-file]]
            [helseplan.datasource :as datasource]
            [helseplan.db :as db]))



(defresource medlemmer-resource
  :available-media-types ["text/html" "application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [ctx]  (let [media-type (get-in ctx [:representation :media-type])
                              medlemmer (db/list-medlemmer (datasource/get-ds))]
                          (condp = media-type
                            "text/html" (render-file "templates/medlemmer.html" {:medlemmer medlemmer :header "Medlemmer"})
                            medlemmer))))



(defroutes routes
  (GET "/" [] "<h2>Hello World</h2>")
  (GET "/main" [] (render-file "templates/main.html" {:header "Helseplan Main"}))
  (GET "/dummy" [] "HELLO DUMMY")
  (ANY "/medlemmer" [] medlemmer-resource))


(def app (-> routes))
