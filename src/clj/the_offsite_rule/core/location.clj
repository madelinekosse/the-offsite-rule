(ns the-offsite-rule.core.location
  (:require [clojure.spec.alpha :as s]))

(defn- postcode? [input]
  (re-matches #"[A-Z]{1,2}\d{1,2}\s\d[A-Z]{2}" input))

(s/def ::postcode (s/and string? postcode?))

(s/def ::latitude (s/and float?
                         #(<= -90 % 90)))

(s/def ::longitude (s/and float?
                          #(<= -180 % 180)))

(s/def ::coordinates (s/keys :req [::latitude
                                   ::longitude]))
(s/def ::name string?)

(s/def ::location (s/keys :req [::coordinates]
                                 :opt [::name
                                       ::postcode]))

(defprotocol LocationConverter
  (postcode-coordinates [_ postcode] "Returns coordinate map"))

(defn from-postcode [postcode converter]
  {:pre [(s/valid? ::postcode postcode)
         (satisfies? LocationConverter converter)]
   :post [(s/valid? ::location %)]}
  {::postcode postcode
   ::coordinates (postcode-coordinates converter postcode)})

(defn from-coordinates [lat lng]
  {:post [(s/valid? ::location %)]}
  {::coordinates {::latitude (float lat)
                  ::longitude (float lng)}})
