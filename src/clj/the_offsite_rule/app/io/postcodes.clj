(ns the-offsite-rule.app.io.postcodes
  (:require [the-offsite-rule.app.io.http :as http]
            [the-offsite-rule.app.participants :refer [PostcodeConverter]]
            [the-offsite-rule.app.io.exceptions :as ex]
            [clojure.string :as string]))

(def url "https://api.postcodes.io/postcodes")

;;TODO: handle error response
(defn- query [postcodes]
  (try
    (-> url
        (http/post {:postcodes postcodes})
        (get "result"))
    (catch Exception e
      (throw (ex/network-exception "Failed to conneect to postcode API")))))

(defn- resmap->coords [location-result]
  (let [postcode (get location-result "query")
        location (get location-result "result")]
    {postcode (if (nil? location)
                nil
                {:latitude (get location "latitude") :longitude (get location "longitude")})}))

(defn- response->lookup [response]
  "Convert the response to a lookup or throw error if any are missing"
  (let [res (map resmap->coords response)
        invalid (filter #(every? nil? (vals %)) res)]
    (if (empty? invalid)
      (apply merge res)
      (->> invalid
           (map keys)
           (apply concat)
           (string/join ", ")
           (str "Can't find location for postcodes: ")
           (ex/bad-input-exception)
           throw))))

(defn- postcode-lookup [postcodes]
  (-> postcodes
      query
      response->lookup))

;;TODO: we can initialise with local URL for easier testing!

(defrecord Converter []
  PostcodeConverter
  (-postcode-lookup [self postcodes] (postcode-lookup postcodes)))
