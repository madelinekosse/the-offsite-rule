(ns the-offsite-rule.io.mocks
  (:require [clj-time.core :as t]
            [the-offsite-rule.core
             [event :as event]
             [leg :as leg]
             [location :refer [LocationConverter] :as location]
             [event-location :refer [RouteFinder] :as event-location]
             [search :refer [CityFinder] :as search]]
            [the-offsite-rule.api.event :refer [EventGetter]]))


(defrecord MockLocationConverter [postcode-map]
    LocationConverter
    (postcode-coordinates
      [self postcode]
      (let [coords (get postcode-map postcode)]
        {::location/latitude (first coords)
         ::location/longitude (last coords)})))

(def made-up-station #::location{:coordinates #::location{:latitude 50.0
                                                          :longitude 0.0}
                                 :name "Station"})
(defrecord MockRouteFinder [from to time]
  RouteFinder
  (route [this from to arrival-time] [#::leg{:start-location from
                                             :end-location made-up-station
                                             :transport-type "walk"
                                             :start-time (t/minus time (t/minutes 30))
                                             :end-time (t/minus time (t/minutes 20))}
                                      #::leg{:start-location made-up-station
                                             :end-location to
                                             :transport-type "train"
                                             :start-time (t/minus time (t/minutes 20))
                                             :end-time (t/minus time (t/minutes 5))}]))
