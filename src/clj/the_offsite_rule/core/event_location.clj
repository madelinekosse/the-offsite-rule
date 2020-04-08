(ns the-offsite-rule.core.event-location
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.core
             [event :as event]
             [journey :as journey]
             [location :as location]]
            [the-offsite-rule.core.leg :as leg]))

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

(defn- add-duration-to-route [route]
  (assoc route ::journey/duration-minutes (journey/total-time-minutes route)))

(defn- add-route-to-participant [participant destination arrival-time route-finder]
  {:pre [(s/valid? ::location/location destination)
         (s/valid? inst? arrival-time)]
   :post [(contains? % ::journey/route)]} ;;maybe dont need this now we have spec
  (let [route (route route-finder
                     (::location/location participant)
                     destination
                     arrival-time)]
    (assoc participant ::journey/route route)))

(defn- add-all-routes [participants destination arrival-time route-finder]
  (map (fn[p] (add-route-to-participant p destination arrival-time route-finder)) participants))

(defn- add-routes-to-event [event location route-finder]
  (update event
          ::event/participants
          add-all-routes
          location
          (::event/time event)
          route-finder))

(defn- travel-time-for [participant]
  (let [legs (::journey/route participant)]
    (println "spec not valid")
    (println (s/explain-data ::journey/route legs))
    (journey/total-time-minutes legs)))

(defn- add-total-travel-time [event-location]
  (let [mins (->> event-location
                  ::event/participants
                  (map travel-time-for)
                  (apply +)
                  )]
    (assoc event-location ::total-travel-minutes mins)))

(defn add-routes [event location route-finder]
  {:pre [(s/valid? ::event/event event)
         (s/valid? ::location/location location)
         (satisfies? RouteFinder route-finder)]
   :post [(s/valid? ::event %)]}
  (-> event
      (add-routes-to-event location route-finder)
      add-total-travel-time
      (assoc ::location/location location)))
