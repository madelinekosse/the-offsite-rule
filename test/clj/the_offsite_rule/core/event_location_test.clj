(ns the-offsite-rule.core.event-location-test
  (:require
   [the-offsite-rule.core.event-location :refer [RouteFinder] :as sut]
   [the-offsite-rule.core
    [location :as location]
    [event :as event]
    [leg :as leg]
    [journey :as journey]]
   [clj-time.core :as t]
   [clojure.test :refer :all]
   [clojure.spec.alpha :as s]))

(def sample-route [{::leg/start-location
                    {::location/coordinates
                     {::location/latitude 0.0
                      ::location/longitude 0.0}}
                    ::leg/end-location
                    {::location/coordinates
                     {::location/latitude 90.0
                      ::location/longitude 90.0}}
                    ::leg/transport-type "train"
                    ::leg/start-time (t/date-time 2020 1 1 9)
                    ::leg/end-time (t/date-time 2020 1 1 9 30)}])

(defrecord MockRouteFinder [r]
  RouteFinder
  (route [this from to arrival-time] r))

(def sample-route-finder (->MockRouteFinder sample-route))

(def sample-participant {::event/name "mk"
                         ::location/location
                         {::location/coordinates
                          {::location/longitude 0.0
                           ::location/latitude 0.0}
                          ::location/postcode "N4 3LR"}})

(def sample-event {::event/participants
                   [sample-participant]
                   ::event/time (t/date-time 2020 1 1 10)})

(def sample-location {::location/coordinates
                      {::location/latitude 90.0
                       ::location/longitude 90.0}})

(deftest test-add-all-routes
  (let [expected-participants [(assoc
                                sample-participant
                                ::journey/route sample-route)]
        result (sut/add-routes
                sample-event
                sample-location
                sample-route-finder)]
    (testing "Routes are added for participants"
      (is (= (::event/participants result)
             expected-participants)))
    (testing "Correct total travel time"
      (is (= (::sut/total-travel-minutes result) 30)))))
