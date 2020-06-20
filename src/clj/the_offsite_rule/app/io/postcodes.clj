(ns the-offsite-rule.app.io.postcodes
  (:require [the-offsite-rule.app.io.http :as http]
            [the-offsite-rule.app.participants :refer [PostcodeConverter]]
            [the-offsite-rule.app.io.exceptions :as ex]
            [clojure.string :as string]))

(def url "https://api.postcodes.io/postcodes")

(defn- query [postcodes]
  (try
    (-> url
        (http/post {:postcodes postcodes})
        (get "result"))
    (catch Exception e
      (throw (ex/network-exception "Failed to conneect to postcode API")))))

(defn- find-nearby-postcode [postcode]
  "For a postcode that isn't found, look up another one with the same prefix and use that"
  (let [prefix (first (string/split postcode #" "))]
    (try (let [alternative (-> url
                               (str "/" prefix "/autocomplete")
                               (http/get {})
                               (get "result")
                               first)]
           {:original postcode
            :alternative alternative})
       (catch Exception e
         (throw
          (ex/network-exception "Failed to connect to postcode API"))))))

(defn- search-alternative-postcode [{:keys [original alternative]}]
  "Look up location data for alternative potcode and update te query field with original one"
  (-> alternative
      vector
      query
      first
      (assoc "query" original)))

(defn- replace-missing-postcodes [query-response-rows]
  "For any rows with no location, find a nearby postcode and use the location data for that"
  (let [good-rows (filter (fn[r] (some? (get r "result"))) query-response-rows)]
    (->> query-response-rows
         (filter (fn[r] (nil? (get r "result"))))
         (map #(get % "query"))
         (map find-nearby-postcode)
         (map search-alternative-postcode)
         (concat good-rows))))


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
      replace-missing-postcodes
      response->lookup))

;;TODO: we can initialise with local URL for easier testing!

(defrecord Converter []
  PostcodeConverter
  (-postcode-lookup [self postcodes] (postcode-lookup postcodes)))
