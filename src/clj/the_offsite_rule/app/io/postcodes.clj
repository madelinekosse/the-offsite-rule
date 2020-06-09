(ns the-offsite-rule.app.io.postcodes
  (:require [the-offsite-rule.app.io.http :as http]
            [the-offsite-rule.app.participants :refer [PostcodeConverter]]))

(def url "https://api.postcodes.io/postcodes")

;;TODO: handle error response
(defn- query [postcodes]
  (-> url
      (http/post {:postcodes postcodes})
      (get "result")))

(defn- resmap->coords [location-result]
  (let [postcode (get location-result "query")
        location (get location-result "result")]
    {postcode {:latitude (get location "latitude") :longitude (get location "longitude")}}))

(defn- response->lookup [response]
  (->> response
       (map resmap->coords)
       (apply merge)))

(defrecord Converter []
  PostcodeConverter
  (-postcode-lookup [self postcodes] (-> postcodes
                                         query
                                         response->lookup)))
