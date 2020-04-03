(ns the-offsite-rule.core.event-location
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.core
             [event :as event]
             [journey :as journey]
             [location :as location]]))

(s/def ::total-travel-minutes #(s/valid? ::journey/duration-minutes %))

(defn- route-for-all? [event]
  (->> event
       ::event/participants
       (map ::journey/route)
       (every? some?)))

(s/def ::event (s/and (s/merge ::event/event
                               (s/keys :req [::total-travel-minutes ::location/location]))
                      route-for-all?))

(defprotocol RouteFinder
  (route [_ from to arrival-time] "returns fastest route from one location to another arriving by given time"))

(defn- add-route-to-participant [participant destination arrival-time route-finder]
  {:pre [(s/valid? ::event/participant participant)
         (s/valid? ::location/location destination)
         (s/valid? inst? arrival-time)]
   :post [(s/valid? ::event/participant %)
          (contains? % ::journey/route)]} ;;maybe dont need this now we have spec
  (let [participant-coordinates (get-in participant [::location/location ::location/coordinates])
        destination-coordinates (::location/coordinates destination)]
    (assoc participant ::journey/route (route route-finder
                                              participant-coordinates
                                              destination-coordinates
                                              arrival-time))))

(defn- add-all-routes [participants destination arrival-time route-finder]
  (map (fn[p] (add-route-to-participant p destination arrival-time route-finder)) participants))

(defn- add-routes-to-event [event location route-finder]
  (update event
          ::event/participants
          add-all-routes
          location
          (::event/time event)
          route-finder))

(defn- add-total-travel-time [route]
  (let [mins (->> route
                  ::event/participants
                  (map ::journey/route)
                  (map journey/total-time-minutes)
                  (apply +))]
    (assoc route ::total-travel-minutes mins)))

(defn add-routes [event location route-finder]
  {:pre [(s/valid? ::event/event event)
         (s/valid? ::location/location location)
         (satisfies? RouteFinder route-finder)]
   :post [(s/valid? ::event %)]}
  (-> event
      (add-routes-to-event location route-finder)
      add-total-travel-time
      (assoc ::location/location location)))
