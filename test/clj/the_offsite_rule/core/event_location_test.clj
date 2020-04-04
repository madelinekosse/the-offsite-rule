(ns the-offsite-rule.core.event-location-test
  (:require
   [the-offsite-rule.core.event-location :refer [RouteFinder] :as sut]
   [the-offsite-rule.core
    [location :as location]
    [event :as event]
    [leg :as leg]
    [journey :as journey]]
   [the-offsite-rule.io.mocks :as io]
   [clj-time.core :as t]
   [clojure.test :refer :all]
   [clojure.spec.alpha :as s]))

(def from-location (location/from-coordinates 0.0 0.0))
(def to-location (location/from-coordinates 50.0 50.0))
(def participant {::event/name "MK"
                  ::location/location from-location})

(def start-time (t/date-time 2020 1 1 9))

(def event (event/event start-time
                        [participant]))
(def route-finder (io/->MockRouteFinder from-location
                                        to-location
                                        start-time))

(deftest test-add-all-routes
  (let [result (sut/add-routes
                event
                to-location
                route-finder)
        participant-route (-> result
                              ::event/participants
                              first
                              ::journey/route)]
    (testing "Route for participant starts at their home location"
      (is (= from-location
             (::leg/start-location (first participant-route)))))
    (testing "Route ends at end location"
      (is (= to-location
             (::leg/end-location (last participant-route)))))
    (testing "Correct total travel time excludes waiting at destination"
      (is (= 25
             (::sut/total-travel-minutes result))))))
