(ns helseplan.controllers.medlem
  (:require
   [liberator.core :refer [resource defresource by-method]]
   [selmer.parser :refer [render-file]]
   [helseplan.datasource :as ds]
   [helseplan.services.medlem :as service]
   [helseplan.domain.medlem :as domain]
   [helseplan.controllers.helper :as helper]
   [helseplan.controllers.auth :as auth]
   [helseplan.controllers.helper :as helper :refer [media-typed location-flash]]))


(defn medlem-fra-request [ctx]
  (get-in ctx [:request :params]))


(defn vis-medlemmer
  ([ctx] (vis-medlemmer ctx (get-in ctx [:request :flash])))
  ([ctx message]
  (let [media-type (get-in ctx [:representation :media-type])
        medlemmer (service/finn (ds/get-ds))]
    (condp = media-type
      "text/html" (render-file "templates/medlemmer.html" {:medlemmer medlemmer
                                                           :header "Medlemmer"
                                                           :message message})
      medlemmer))))

(defn gyldig-medlem? [ctx]
  (println (medlem-fra-request ctx))
  (let [params (medlem-fra-request ctx)]
    (println (domain/check-medlem params))
    (not (:error (domain/check-medlem params)))))

(defn vis-ugyldig-ny-medlem [ctx]
  (let [params (medlem-fra-request ctx)
        errors (domain/check-medlem params)
        media-type (get-in ctx [:representation :media-type])]
    (condp = media-type
      "text/html" (render-file "templates/medlem.html" {:medlem params
                                                        :header "Ny medlem"
                                                        :method "POST"
                                                        :action "http://localhost:3000/medlemmer"
                                                        :errors (:error errors)})
      {:errors (:error errors)})))

(defn ny-medlem [ctx]
  (let [params (medlem-fra-request ctx)]
    (try ; TODO: Må finne ut hvordan man kan få ut exception info fra liberator !
      (let [id (service/ny (ds/get-ds) (dissoc params (keyword "__method")))]
        {::id id})
      (catch Exception e
        (println e)))))

(defn vis-medlem [id ctx]
  (let [media-type (get-in ctx [:representation :media-type])
        medlem (:medlem ctx)]
    (condp = media-type
      "text/html" (render-file "templates/medlem.html" {:medlem medlem :header "Endre medlem"})
      medlem)))

(defn vis-ugyldig-endre-medlem [ctx]
  (let [params (medlem-fra-request ctx)
        errors (domain/check-medlem params)]
    (render-file "templates/medlem.html" {:medlem params
                                          :header "Endre medlem"
                                          :errors (:error errors)})))
(defn oppdater-medlem [ctx]
  (let [params (medlem-fra-request ctx)]
    (try
      (service/oppdater (ds/get-ds) (dissoc params (keyword "__method")))
      (catch Exception e
        (println e))) ; TODO: Må finne ut hvordan man kan få ut exception info fra liberator !
    ))

(defn etter-medlem-oppdatert [ctx]
  (let [params (medlem-fra-request ctx)]
    (render-file "templates/medlem.html" {:medlem params
                                          :header "Endre medlem"
                                          :method ""})))

(defn slett-medlem [id ctx]
  (try
    (service/slett (ds/get-ds) id)
    (catch Exception e
      (println e)))
  )

(defn etter-medlem-slettet [id ctx]
  ;; "Jeg vet ikke om dette er riktig eller helt feil, men det funker."
  (vis-medlemmer ctx (clojure.string/replace "Medlem med id=$id ble slettet" "$id" id)))


(defn admin? [ctx]
  (cemerick.friend/authorized? #{:admin} (cemerick.friend/identity (get ctx :request))))


(defresource medlemmer-resource
  auth/friend-resource
  :available-media-types       ["text/html" "application/json"]
  :allowed-methods             [:get :post]
  :allowed?                    (by-method {:get true
                                           :post admin?})
  :handle-ok                   vis-medlemmer
  :processable?                (by-method {:get true :post gyldig-medlem?})
  :handle-unprocessable-entity vis-ugyldig-ny-medlem
  :handle-exception            (fn [{ex :exception}]
                                 (clojure.stacktrace/print-stack-trace ex)
                                 (str "error: " (.getMessage ex)))
  :post!                       ny-medlem
  :post-redirect?              (fn [ctx] {:location (format "/medlemmer/%s" (::id ctx))}))


(defresource medlem-resource [id]
  auth/friend-resource
  :available-media-types           ["text/html" "application/json"]
  :allowed-methods                 [:get :put :delete]
  :allowed?                        (by-method {:get true
                                               :put admin?
                                               :delete admin?})
  :exists?                         (fn [ctx]
                                     (when-let [medlem (service/hent (ds/get-ds) id)]
                                       {:medlem medlem}))
  :handle-not-found                (media-typed {"text/html" (location-flash "/medlemmer" (str "Medlem med id " id " finnes ikke"))
                                      "application/json"
                                      {:success false
                                       :message "Ikke funnet"}
                                      :default (constantly "Ikke funnet")})
  :handle-ok                       (partial vis-medlem id)
  :processable?                    (by-method {:get true :put gyldig-medlem? :delete true})
  :handle-unprocessable-entity     vis-ugyldig-endre-medlem
  :handle-exception                (fn [{ex :exception}]
                                     (println ex)
                                     (str "error: " (.getMessage ex)))
  :put!                            oppdater-medlem
  :handle-created                  etter-medlem-oppdatert
  :delete!                         (partial slett-medlem id)
  :respond-with-entity?            (by-method {:get false :put false :delete true})
  :multiple-representations?       (by-method {:get false :put false :delete true})
  :handle-multiple-representations (partial etter-medlem-slettet id))
