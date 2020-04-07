(ns the-offsite-rule.core.location
  (:require [clojure.spec.alpha :as s]
            [clojure.math.numeric-tower :as math]
            [the-offsite-rule.core.location :as location]))

(defn- postcode? [input]
  (re-matches #"[A-Z]{1,2}\d{1,2}\s\d[A-Z]{2}" input))

(s/def ::postcode (s/and string? postcode?))

(s/def ::latitude (s/and float?
                         #(<= -90 % 90)))

(s/def ::longitude (s/and float?
                          #(<= -180 % 180)))

(s/def ::coordinates (s/keys :req [::latitude
                                   ::longitude]))

(s/def ::name (fn [n] (or (string? n)
                          (nil? n))))

(s/def ::location (s/keys :req [::coordinates]
                                 :opt [::name
                                       ::postcode]))

(defprotocol LocationConverter
  (postcode-coordinates [_ postcode] "Returns coordinate map"))

(defn from-postcode [postcode converter]
  {:pre [(s/valid? ::postcode postcode)
         (satisfies? LocationConverter converter)]
   :post [(s/valid? ::location %)]
   }
  {::postcode postcode
   ::coordinates (postcode-coordinates converter postcode)})

(defn from-coordinates [lat lng]
  {:post [(s/valid? ::location %)]}
  {::coordinates {::latitude (float lat)
                  ::longitude (float lng)}})

(defn center [locations]
  {:pre [(s/valid? (s/every ::location) locations)]
   :post [(s/valid? ::coordinates %)]}
  (let [latitudes (map #(get-in % [::coordinates ::latitude]) locations)
        longitudes (map #(get-in % [::coordinates ::longitude]) locations)]
    {::latitude (/ (apply + latitudes) (count locations))
      ::longitude (/ (apply + longitudes) (count locations))}))

(defn distance [location coordinates]
  {:pre [(s/valid? ::location location)
         (s/valid? ::coordinates coordinates)]
   :post [(number? %)]}
  (let [lat1 (get-in location [::coordinates ::latitude])
        lng1 (get-in location [::coordinates ::longitude])
        lat2 (::latitude coordinates)
        lng2 (::longitude coordinates)]
    (math/sqrt (+ (math/expt (- lat1 lat2) 2)
                (math/expt (- lng1 lng2) 2)))))
