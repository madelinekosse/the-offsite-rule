(ns the-offsite-rule.app.location
  (:require
   [the-offsite-rule.event :as e]
   [the-offsite-rule.location :as l]
   [clojure.spec.alpha :as s]
   [the-offsite-rule.value :as value]))

(defn total-journey-time [location-with-routes]
  {:pre [(s/valid? ::e/location location-with-routes)]
   :post [(s/valid? ::value/duration %)]}
  (::e/total-journey-time location-with-routes))

(defn name [loc]
  {:pre [(s/valid? ::l/location loc)]
   :post [(s/valid? ::l/name %)]}
  (::l/name loc))

(defn routes [location-with-routes]
  {:pre [(s/valid? ::e/location location-with-routes)]
   :post [(s/valid? ::e/routes %)]}
  (::e/routes location-with-routes))
