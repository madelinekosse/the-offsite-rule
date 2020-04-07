(ns the-offsite-rule.core.search
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.core
             [location :as location]
             [event :as event]
             [event-location :refer [RouteFinder]:as candidate]]))

(defprotocol CityFinder
  (locations [_] "Return a list of all possible locations"))


(s/def ::candidates (s/every ::candidate/event))

(s/def ::location-list (s/every ::location/location))

(defn- get-potential-locations [event finder]
  "Fetches locations from finder and sorts by distance from midpoint"
  {:pre [(satisfies? CityFinder finder)
         (s/valid? ::event/event event)]
   ;;:post [(s/valid? ::location-list %)] ;; Why doesn't this find it?
   }
  (let [midpoint (event/midpoint event)]
    (->> (locations finder)
         (map (fn [l] (assoc l :distance (location/distance l midpoint))))
         (sort-by :distance)
         (map (fn [l] (dissoc l :distance))))))

;;TODO: converge when routes stop getting shorter not just at cutoff point
(defn candidate-events [event locations route-finder]
  (let [locations (take 10 locations)]
    (->> locations
         (map (fn [l] (candidate/add-routes event l route-finder))
              )
         (sort-by ::candidate/total-travel-minutes))))

(defn best-locations [event city-finder route-finder]
  (let [locations (get-potential-locations event city-finder)]
    (candidate-events event locations route-finder)))
