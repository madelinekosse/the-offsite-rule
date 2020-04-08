(ns the-offsite-rule.api.search
  (:require [the-offsite-rule.core
             [search :as search]
             [event-location :as event-location]
             [event :as event]
             [location :as location]
             [participant :as participant]
             [journey :as journey]]))

(defn search-locations [{:keys [event city-finder route-finder postcode-converter] :as state}]
  "Add a list of event locations to the state"
  (assoc state :locations (search/best-locations event city-finder route-finder)))

(defn- journey-summary [participant]
  (let [name (::participant/name participant)
        mins (journey/total-time-minutes (::journey/route participant))]
    {:name name
     :travel-time mins}))

(defn- location-summary [candidate]
  {:total-time (::event-location/total-travel-minutes candidate)
   :name (get-in candidate [::location/location ::location/name])
   :journeys (map journey-summary (::event/participants candidate))})

(defn list-best-location-times [{:keys [locations] :as state}]
  (map location-summary locations))
