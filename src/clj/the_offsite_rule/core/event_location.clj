(ns the-offsite-rule.core.event-location
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.core
             [event :as event]
             [journey :as journey]
             [location :as location]]))

(defprotocol RouteFinder
  (route [_ from to arrival-time] "returns fastest route from one location to another arriving by given time"))


(defn- add-route-to-participant [participant destination arrival-time route-finder]
  {:pre [(s/valid? ::event/participant participant)
         (s/valid? ::location/location destination)
         (s/valid? inst? arrival-time)]
   :post [
          ;;(s/valid? ::event/participant %)
          (contains? % ::journey/route)]}
  (let [participant-coordinates (get-in participant [::location/location ::location/coordinates])
        destination-coordinates (::location/coordinates destination)]
    (assoc participant ::journey/route (route route-finder
                                              participant-coordinates
                                              destination-coordinates
                                              arrival-time))))

(defn- add-all-routes [participants destination arrival-time route-finder]
  (map (fn[p] (add-route-to-participant p destination arrival-time route-finder)) participants))

(defn add-routes [event location route-finder]
  {:pre [(s/valid? ::event/event event)
         (s/valid? ::location/location location)
         (satisfies? RouteFinder route-finder)
         ]
   ;;:post [(s/valid? ::event/event %)]

   }
  (update event ::event/participants add-all-routes location (::event/time event) route-finder))
