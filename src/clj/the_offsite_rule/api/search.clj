(ns the-offsite-rule.api.search
  (:require [the-offsite-rule.core
             [search :as search]
             [event-location :as event-location]]
            [the-offsite-rule.core.location :as location]))

(defn search-locations [{:keys [event city-finder route-finder postcode-converter] :as state}]
  "Add a list of event locations to the state"
  (assoc state :locations (search/best-locations event city-finder route-finder)))

(defn- location-summary [candidate]
  {:total-time (::event-location/total-travel-minutes candidate)
   :name (get-in candidate [::location/location ::location/name])})

(defn list-best-location-times [{:keys [locations] :as state}]
  (map location-summary locations))
