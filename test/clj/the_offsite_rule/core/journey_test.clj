(ns the-offsite-rule.core.journey-test
  (:require [the-offsite-rule.core.journey :as sut]
            [the-offsite-rule.core
             [leg :as leg]
             [location :as location]]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [clojure.spec.alpha :as s]))

(def sample-route [{::leg/start-location {::location/coordinates {::location/latitude 0.0
                                                                  ::location/longitude 0.0}}
                    ::leg/end-location {::location/coordinates {::location/latitude 1.0
                                                                ::location/longitude 1.0}}
                    ::leg/transport-type "walk"
                    ::leg/start-time (t/date-time 2020 1 1 9)
                    ::leg/end-time (t/date-time 2020 1 1 9 10)}
                   {::leg/start-location {::location/coordinates {::location/latitude 1.0
                                                                  ::location/longitude 1.0}}
                    ::leg/end-location {::location/coordinates {::location/latitude 2.0
                                                                ::location/longitude 2.0}}
                    ::leg/transport-type "train"
                    ::leg/start-time (t/date-time 2020 1 1 9 10)
                    ::leg/end-time (t/date-time 2020 1 1 9 20)}])

(deftest test-total-route-time
  (testing "Route time correctly calculated"
    (is (= (sut/total-time-minutes sample-route)
           20))))
