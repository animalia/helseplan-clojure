(ns helseplan.controllers.auth
  (:require [cemerick.friend :as friend]
            [helseplan.controllers.helper :refer [media-typed]]))



(defn unauthorized!
  "Throws the proper unauthorized! slingshot error if authentication
  fails. This error is picked up upstream by friend's middleware."
  [handler req]
  (println "Here we are !")
  (friend/throw-unauthorized (friend/identity req)
                             {::friend/wrapped-handler handler}))

(defn roles
  "Returns an authorization function that checks if the authenticated
  user has the specified roles. (This is the usual friend behavior.)"
  [roles]
  (fn [id]
    (friend/authorized? roles id)))

((roles ::helseplan.web/admin) "dill")


(def friend-resource
  "Base resource that will handle authentication via friend's
  mechanisms. Provide an authorization function and you'll be good to
  go."
  {:handle-unauthorized
   (media-typed {"text/html" (fn [req]
                               (unauthorized!
                                (-> req :resource :allowed?)
                                req))
                 "application/json"
                 {:success false
                  :message "Not authorized!"}
                 :default (constantly "Not authorized.")})})
