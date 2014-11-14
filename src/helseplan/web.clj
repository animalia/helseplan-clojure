(ns helseplan.web
  (:require [compojure.core :refer [defroutes GET ANY]]
            [compojure.handler :as handler ]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as ring]
            [ring.util.response :as resp]
            [selmer.parser :refer [render-file]]
            [helseplan.controllers.medlem :as medlem-controller]
            [helseplan.ddl :as ddl]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))



(def users {"root" {:username "root"
                    :password (creds/hash-bcrypt "admin_password")
                    :roles #{::admin}}
            "jane" {:username "jane"
                    :password (creds/hash-bcrypt "test")
                    :roles #{::user}}})


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
  (ANY "/medlemmer" [] medlem-controller/medlemmer-resource)
  (GET "/login" [] (render-file "templates/login.html" {}))
  (GET "/logout" req (friend/logout* (resp/redirect (str (:context req) "/"))))
  (GET "/role-admin" req (friend/authorize #{::admin} "You're an admin!")))



(def app (-> (handler/site
              (friend/authenticate
               routes
               {:credential-fn (partial creds/bcrypt-credential-fn users)
                :workflows [(workflows/interactive-form)]
                :login-uri "/login"}))
             wrap-fake-methods
             wrap-params))


(defn bootstrap []
  (ddl/create-schema! (helseplan.datasource/get-ds))
  (ddl/create-sample-data! (helseplan.datasource/get-ds)))
