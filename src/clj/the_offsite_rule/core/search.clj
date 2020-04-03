(ns the-offsite-rule.core.search
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t] ;;remove
            [the-offsite-rule.core
             [location :as location]
             [event :as event]
             [event-location :refer [RouteFinder]:as candidate]]))

(defprotocol CityFinder
  (locations [_] "Return a list of all possible locations"))


(s/def ::candidates (s/every ::candidate/event))

(s/def ::location-list (s/every ::location/location))

(defn- get-potential-locations [finder event]
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
