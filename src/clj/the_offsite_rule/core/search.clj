(ns the-offsite-rule.core.search
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.core
             [location :as location]
             [event :as event]]
            [the-offsite-rule.core.event-location :refer [RouteFinder]:as candidate]))

(defprotocol CityFinder
  (locations [_] "Return a list of all possible locations"))


(s/def ::candidates (s/every ::candidate/event))

(s/def ::location-list (s/every ::location/location))

(defn- get-potential-locations [event finder]
  {:pre [(satisfies? CityFinder finder)
         (s/valid? ::event/event event)]
   ;;:post [(s/valid? ::location-list %)] ;; Why doesn't this find it?
   }
  "Fetches locations from finder and sorts by distance from midpoint"
  (let [midpoint (event/midpoint event)]
    (->> (locations finder)
         (map (fn [l] (assoc l :distance (location/distance l midpoint))))
         (sort-by :distance)
         (map (fn [l] (dissoc l :distance))))))

(defn candidate-events [sorted-locations event route-finder]
  (loop [results []
         location-index 0
         shortest-time Long/MAX_VALUE
         fail-streak-length 0]
    (let [
          next-location (nth sorted-locations location-index)
          next-candidate (candidate/add-routes event next-location route-finder)
          next-candidate-time (::candidate/total-travel-minutes next-candidate)]
      (cond
        (= location-index (dec (count sorted-locations))) (conj results next-candidate)
        (< next-candidate-time shortest-time) (recur (conj results next-candidate)
                                                     (inc location-index)
                                                     next-candidate-time
                                                     0)
        (>= fail-streak-length 5) results
        :else (recur (conj results next-candidate)
                     (inc location-index)
                     shortest-time
                     (inc fail-streak-length))))))

(defn best-locations [event city-finder route-finder]
  (let [results (-> event
                    (get-potential-locations city-finder)
                    (candidate-events event route-finder))]
    (sort-by ::candidate/total-travel-minutes results)))
