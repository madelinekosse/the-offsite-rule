(ns the-offsite-rule.participant
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule
             [location :as l]
             [value :as value]]))

(s/def ::name #(s/valid? ::value/name %))

;; Participant locations must contain postcode
(s/def ::location #(and (s/valid? ::l/location %)
                        (contains? % ::l/postcode)))

(s/def ::participant (s/keys :req [::name
                                   ::location]))

;; TODO: as with event, we may want the constructor in next layer so havent added tests yet
(defn participant [name location]
  {:pre [(s/valid? ::name name)
         (s/valid? ::l/location location)]
   :post [(s/valid? ::participant %)]}
  "Return a new participant with given location and name as namespaced keys"
  {::name name
   ::location location})
