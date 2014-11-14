(ns helseplan.web
  (:require [compojure.core :refer [defroutes GET ANY]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as ring]
            [selmer.parser :refer [render-file]]
            [helseplan.controllers.medlem :as medlem-controller]))


(defn wrap-fake-methods
  "Middleware to wrap a handler to use the HTTP request method
  specified in the __method query or post parameter if it exists.
  Note that this middleware must be called after the query and/or post
  parameters are parsed using ring.middleware/wrap-params or similar."
  [hdlr]
  (fn [req]

    (if-let [request-method (and (= :post (:request-method req))
                                 (or (get-in req [:params :__method])
                                     (get-in req [:params "__method"])))]
      (hdlr (assoc req :request-method (keyword (clojure.string/lower-case request-method))))
      (hdlr req))))




(defroutes routes
  (GET "/" [] "<h2>Hello World</h2>")
  (GET "/main" [] (render-file "templates/main.html" {:header "Helseplan Main"}))
  (GET "/medlemmer/ny" [] (render-file "templates/medlem.html" {:medlem {}
                                                            :header "Nytt medlem"
                                                            :method "POST"
                                                            :action "http://localhost:3000/medlemmer"}))
  (ANY ["/medlemmer/:id"] [id] (medlem-controller/medlem-resource id))
  (ANY "/medlemmer" [] medlem-controller/medlemmer-resource))



(def app (-> routes
             wrap-fake-methods
             wrap-params))
