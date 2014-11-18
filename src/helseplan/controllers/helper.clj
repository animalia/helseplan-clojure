(ns helseplan.controllers.helper
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [liberator.core :as l]
            [compojure.route :as route]
            [liberator.conneg :as conneg]
            [liberator.representation :as rep]
            [ring.util.response :as r])
  (:import [liberator.representation RingResponse]))

(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json [context key]
  (when (and (#{:put :post} (get-in context [:request :request-method]))
             (= "application/json" (get-in context [:request :content-type])))
    (try
      (if-let [body (body-as-string context)]
        (let [data (json/read-str body)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))



(defn accepted-types
  "Returns a sequence of content types accepted by the supplied
  request. If no accept header is present, returns nil."
  [req]
  (when-let [accepts-header (get-in req [:headers "accept"])]
    (->> (conneg/sorted-accept accepts-header ["*/*"])
         (map (comp conneg/stringify :type))
         (filter not-empty))))

(defn get-media
  "Pulls the media type out of the request, or parses it from the
  content headers.
  allowed-types is a set containing pairs (e.g., [\"text\" \"*\"])
  or strings (e.g., \"text/plain\").
  If no allowed-types is present, returns the type most favored by the
  client."
  ([req]
     (first (accepted-types req)))
  ([req allowed-types]
     {:pre [(contains? (:headers req) "accept")
            (sequential? allowed-types)]}
     (l/try-header "Accept"
                   (when-let [accept-header (get-in req [:headers "accept"])]
                     (let [type (conneg/best-allowed-content-type
                                 accept-header
                                 allowed-types)]
                       (not-empty (conneg/stringify type)))))))

;;(def ring-response rep/ring-response)
(def ringify
  (comp rep/ring-response r/response))

(defn to-response
  "to-response does more intelligent response parsing on your
  liberator ring responses.
  Liberator tries to coerce your returned value into the proper
  content type; maps get turned into json or clojure as required, etc.
  The problem with this, naively, is that ring's responses are ALSO
  just bare maps. If you return a bare map to a text/html request,
  liberator tries to coerce the map into HTML.
  The liberator solution is a special wrapper type called RingResponse
  that gets passed through without diddling. This function handles the
  most common response type cases in one spot
  If you pass in an instance of RingResponse, to-response passes it
  through untouched.
  If you pass in a ring response map, it's wrapped in an instance of
  RingResponse and passed on (and ignored by liberator).
  else, liberator tries to coerce as before."
  [t req]
  (cond (instance? RingResponse t) t
        (r/response? t) (rep/ring-response t)
        :else (rep/ring-response (rep/as-response t req))))
(defn generic
  "If you pass a response back to liberator before it's parsed the
  content type it freaks out and says that it can't dispatch on
  null. This generic method calls the proper multimethod rendering on
  json, clojure, etc, all of that business, before passing the result
  back up the chain through liberator."
  [data req media-type]
  (to-response data (assoc-in req [:representation :media-type] media-type)))

(defn media-typed
  "Accepts a map of encoding -> handler (which can be a constant or a
  function) and returns a NEW handler that delegates properly based on
  the request's encoding. If no encoding is found, calls the handler
  under the :default key."
  [& ms]
  (let [m (apply merge ms)]
    (fn [req]
      (let [get-media #(get-in % [:representation :media-type])
            parsed-type (get-media req)
            media-type (or parsed-type
                           (get-media (l/negotiate-media-type req)))]
        (when-let [handler (get m media-type (:default m))]
          (if (fn? handler)
            (handler req)
            (if-not (= parsed-type media-type)
              (generic handler req media-type)
              (to-response handler req))))))))



(defn location-flash [uri flash]
  {:headers {"Location" uri}
   :status 303
   :flash flash})
