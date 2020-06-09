(ns the-offsite-rule.search
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule
             [participant :as p]
             [location :as l]
             [journey :as j]
             [event :as e]
             [value :as v]]
            [clj-time.core :as t]))

(defprotocol Map
  (-locations [_] "Return a list of locations conforming to location/location")
  (-route [_ from to arrival-time] "returns fastest route (conforming to journey)"))

(s/def ::candidate-locations (s/coll-of ::l/location))

(defn- locations [map]
  {:pre [(satisfies? Map map)]
   :post [(s/valid? ::candidate-locations %)]}
  (-locations map))

(defn- route [map from to arrival-time]
  {:pre [(satisfies? Map map)
         (s/valid? ::l/location from)
         (s/valid? ::l/location to)
         (s/valid? ::v/time arrival-time)]
   :post [(s/valid? ::j/journey %)]}
  (-route map from to arrival-time))

(defn- sort-locations [event locations]
  {:pre [(every? #(s/valid? ::l/location %) locations)
         (s/valid? ::e/event event)]}
  "Sort locations according to their distance from the participant midpoint"
  (let [midpoint (->> event
                      e/participant-locations
                      l/midpoint)]
    (sort-by (partial l/distance midpoint) locations)))

(defn- add-routes-to-location [event map-api location]
  {:pre [(s/valid? ::l/location location)
         (s/valid? ::e/event event)]
   :post [(s/valid? ::e/location %)]}
  "Add routes for all participants and total journey time to location"
  (let [find-route (fn[start-loc]
                     (-route map-api start-loc location (::e/time event)))
        routes (->> event
                    e/participant-locations
                    (map find-route))
        total-time (->> routes
                        (map j/total-time)
                        v/add-durations)]
    (merge location
           {::e/routes routes
            ::e/total-journey-time total-time})))

(defn- find-fastest-routes [event map-api sorted-locations]
  "Search for faster routes until they stop getting faster"
  (loop [results []
         candidates sorted-locations
         fail-count 0
         best-time Long/MAX_VALUE]
    (if (empty? candidates)
      results
      (let [new-result (add-routes-to-location event map-api (first candidates))
            new-result-time (t/in-minutes (::e/total-journey-time new-result))
            [new-fail-count new-best-time] (if (< new-result-time best-time)
                                             [0 new-result-time]
                                             [(inc fail-count) best-time])]
        (if (= new-fail-count 5)
          (conj results new-result)
          (recur (conj results new-result)
                 (rest candidates)
                 new-fail-count
                 new-best-time))))))


(defn best-locations [event map-api]
  {:pre [(s/valid? ::e/event event)]
   :post [(s/valid? ::e/locations %)]}
  "Return a sorted list of event locations for the event"
  (->> map-api
       locations
       (sort-locations event)
       (find-fastest-routes event map-api)
       (sort-by ::e/total-journey-time)))
