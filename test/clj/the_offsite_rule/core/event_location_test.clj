(ns the-offsite-rule.core.event-location-test
  (:require
   [the-offsite-rule.core.event-location :refer [RouteFinder] :as sut]
   [the-offsite-rule.core
    [location :as location]
    [event :as event]
    [journey :as journey]]
   [clj-time.core :as t]
   [clojure.test :refer :all]
   [clojure.spec.alpha :as s]))

(def sample-route [{::journey/start-location
                    {::location/coordinates
                     {::location/latitude 0.0
                      ::location/longitude 0.0}}
                    ::journey/end-location
                    {::location/coordinates
                     {::location/latitude 90.0
                      ::location/longitude 90.0}}
                    ::journey/transport-type "train"
                    ::journey/start-time (t/date-time 2020 1 1 9)
                    ::journey/end-time (t/date-time 2020 1 1 9 30)}])

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
  (testing "Routes are added for participants"
    (let [expected-participants [(assoc
                                  sample-participant
                                  ::journey/route sample-route)]
          result (sut/add-routes
                  sample-event
                  sample-location
                  (->MockRouteFinder sample-route))]
      (is (= (::event/participants result)
             expected-participants)))))
