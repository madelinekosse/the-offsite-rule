(ns the-offsite-rule.location
  (:require [clojure.spec.alpha :as s]
            [clojure.math.numeric-tower :as math]
            [the-offsite-rule.value :as value]))

(defn- postcode? [input]
  (re-matches #"[A-Z]{1,2}\d{1,2}\s\d[A-Z]{2}" input))

(s/def ::postcode (s/and string? postcode?))

(s/def ::latitude (s/and float?
                         #(<= -90 % 90)))

(s/def ::longitude (s/and float?
                          #(<= -180 % 180)))

(s/def ::coordinates (s/keys :req [::latitude
                                   ::longitude]))

(s/def ::name #(s/valid? ::value/name %))

(s/def ::location (s/keys :req [::coordinates]
                          :opt [::name
                                ::postcode]))

(defn- coordinate-location [lat lng]
  {:post [(s/valid? ::location %)]}
  {::coordinates {::latitude (float lat)
                  ::longitude (float lng)}})

(defn- valid-or-nil? [spec value]
  "Return tru iff value is either nil or conforms to spec"
  (if (some? value)
    (s/valid? spec value)
    true))

(defn- location-with-metadata [loc name postcode]
  {:pre [(s/valid? ::location loc)
         (valid-or-nil? ::name name)
         (valid-or-nil? ::postcode postcode)
         ]
   :post [(s/valid? ::location %)]}
  (cond-> loc
    (some? postcode) (assoc ::postcode postcode)
    (some? name) (assoc ::name name)))

(defn location
  ([[lat lng]] (coordinate-location lat lng))
  ([[lat lng] {:keys [:postcode :name]}] (location-with-metadata (coordinate-location lat lng)
                                                                 name
                                                                 postcode)))

(defn midpoint [locations]
  {:pre [(s/valid? (s/every ::location) locations)]
   :post [(s/valid? ::location %)]}
  "Return a new location representing mathematical midpoint of the input locations"
  (let [latitudes (map #(get-in % [::coordinates ::latitude]) locations)
        longitudes (map #(get-in % [::coordinates ::longitude]) locations)]
    (coordinate-location (/ (apply + latitudes) (count locations))
                         (/ (apply + longitudes) (count locations)))))

(defn distance [location1 location2]
  {:pre [(s/valid? ::location location1)
         (s/valid? ::location location2)]
   :post [(float? %)]}
  "Return the pythagorean distance between the two locations (in degrees)"
  (let [lat1 (get-in location1 [::coordinates ::latitude])
        lng1 (get-in location1 [::coordinates ::longitude])
        lat2 (get-in location2 [::coordinates ::latitude])
        lng2 (get-in location2 [::coordinates ::longitude])]
    (math/sqrt (+ (math/expt (- lat1 lat2) 2)
                  (math/expt (- lng1 lng2) 2)))))
