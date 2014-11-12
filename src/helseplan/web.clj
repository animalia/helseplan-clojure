(ns helseplan.web
  (:require [compojure.core :refer [defroutes GET ANY]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as ring]
            [liberator.core :refer [resource defresource by-method]]
            [selmer.parser :refer [render-file]]
            [helseplan.datasource :as datasource]
            [helseplan.db :as db]
            [schema.core :as s]
            [schema.coerce :as coerce]))


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


(defn simple-coercion [schema]
  (s/start-walker
   (fn [sc]
     (let [walk (s/walker sc)]
       (fn [x]
         (if (and (= sc s/Keyword) (string? x))
           (walk (keyword x))
           (walk x)))))
   schema))

(defn length-greater [l]
  (s/pred
   (fn [x]
     (> (count x) l)) (str "Lengden må være større enn " l)))




(def MedlemSchema
  "Skjema for validering av gyldig medlem"
  {:id s/Num
   :fornavn (s/both s/Str (length-greater 0))
   :etternavn (s/both s/Str (length-greater 0))
   :prodnr (s/both s/Str (length-greater 0))
   s/Any s/Any})

(defn parse-medlem-req [data]
  (->>
   data
   ((simple-coercion {s/Keyword s/Any}))
   ((coerce/coercer MedlemSchema coerce/string-coercion-matcher))))



(defresource medlemmer-resource
  :available-media-types ["text/html" "application/json"]
  :allowed-methods [:get]
  :handle-ok (fn [ctx]
               (let [media-type (get-in ctx [:representation :media-type])
                     medlemmer (db/list-medlemmer (datasource/get-ds))]
                 (condp = media-type
                   "text/html" (render-file "templates/medlemmer.html" {:medlemmer medlemmer :header "Medlemmer"})
                   medlemmer))))

(defresource medlem-resource [id]
  :available-media-types ["text/html" "application/json"]
  :allowed-methods [:get :put :post]
  :handle-ok (fn [ctx]
               (let [media-type (get-in ctx [:representation :media-type])
                     medlem (db/hent-medlem (datasource/get-ds) id)]
                 (condp = media-type
                   "text/html" (render-file "templates/medlem.html" {:medlem medlem :header "Endre medlem" :method "PUT"})
                   medlem)))
  :processable?  (by-method {:get true
                             :put (fn [ctx]
                                    (let [params (get-in ctx [:request :params])]
                                      (not (:error (parse-medlem-req params)))))})


  :handle-unprocessable-entity (fn [ctx]
                                 (let [params ((simple-coercion {s/Keyword s/Any})(get-in ctx [:request :params]))
                                       errors (parse-medlem-req params)]
                                   (render-file "templates/medlem.html" {:medlem params
                                                                         :header "Endre medlem"
                                                                         :method "PUT"
                                                                         :errors (:error errors)})))
  :put! (fn [ctx]
          (let [params (get-in ctx [:request :params])]
            (try
              (db/oppdater-medlem! (datasource/get-ds) (dissoc params "__method"))
              (catch Exception e
                (println e))) ; TODO: Må finne ut hvordan man kan få ut exception info fra liberator !
            ))
  :handle-created (fn [ctx]
                    (let [params ((simple-coercion {s/Keyword s/Any})(get-in ctx [:request :params]))]
                      (render-file "templates/medlem.html" {:medlem params
                                                            :header "Endre medlem"
                                                            :method "PUT"})))
  :post-redirect? (fn [ctx] {:location (format "/medlemmer/%s" (::id ctx))})
  :post! (fn [ctx]
           (println ctx)
           "TODO : Oppdatert medlem"))


(defroutes routes
  (GET "/" [] "<h2>Hello World</h2>")
  (GET "/main" [] (render-file "templates/main.html" {:header "Helseplan Main"}))
  (GET "/dummy" [] "HELLO DUMMY")
  (ANY ["/medlemmer/:id"] [id] (medlem-resource id))
  (ANY "/medlemmer" [] medlemmer-resource))



(def app (-> routes
             wrap-fake-methods
             wrap-params))
