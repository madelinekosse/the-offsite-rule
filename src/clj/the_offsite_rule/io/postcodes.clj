(ns the-offsite-rule.io.postcodes
  (:require [the-offsite-rule.io.http :as http]
            [the-offsite-rule.core
             [location :refer [LocationConverter] :as location]]))

(def url "https://api.postcodes.io/postcodes")

(defrecord Converter [postcode-coordinate-map]
  LocationConverter
  (postcode-coordinates [self postcode] (get postcode-coordinate-map postcode)))

(defn- to-map [row]
  (let [postcode (get row "query")
        result (get row "result")]
    {postcode {::location/coordinates {::location/latitude (get result "latitude")
                                       ::location/longitude (get result "longitude")}}}))

;;TODO: handle error response
(defn- query [postcodes]
  (as-> postcodes data
    (assoc {} :postcodes data)
    (http/post url data)
    (get-in data ["result"])))

(defn- coordinate-lookup [postcodes]
  (->> postcodes
       query
       (map to-map)
       (apply merge)))

(defn converter [postcodes]
  (let [lookup (coordinate-lookup postcodes)]
    (->Converter lookup)))
