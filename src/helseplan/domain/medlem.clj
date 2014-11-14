(ns helseplan.domain.medlem
  (:require [schema.core :as s]
            [schema.coerce :as coerce]))


(defn length-greater [l]
  (s/pred
   (fn [x]
     (> (count x) l)) (str "Lengden må være større enn " l)))


(def MedlemSchema
  "Skjema for validering av gyldig medlem"
  {:fornavn (s/both s/Str (length-greater 0))
   :etternavn (s/both s/Str (length-greater 0))
   :prodnr (s/both s/Str (length-greater 0))
   s/Any s/Any})

;; TODO: Flytt ut fra denne !
(defn simple-coercion [schema]
  (s/start-walker
   (fn [sc]
     (let [walk (s/walker sc)]
       (fn [x]
         (if (and (= sc s/Keyword) (string? x))
           (walk (keyword x))
           (walk x)))))
   schema))

;; TODO: Og flytt denne
(defn keywordize [params]
  ((simple-coercion {s/Keyword s/Any}) params))

(defn check-medlem [data]
  ((coerce/coercer MedlemSchema coerce/string-coercion-matcher) data))

(defn parse-medlem-req [data]
  (->>
   data
   ((simple-coercion {s/Keyword s/Any}))
   ((coerce/coercer MedlemSchema coerce/string-coercion-matcher))))
