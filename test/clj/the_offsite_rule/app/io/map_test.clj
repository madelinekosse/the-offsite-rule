(ns the-offsite-rule.app.io.map-test
  (:require [the-offsite-rule.app.io.map :as sut]
            [the-offsite-rule
             [location :as location]
             [journey :as journey]]
            [clj-time.core :as t]
            [clojure.test :refer :all]))

(def home (location/location [1.1 20.2] {:postcode "AA1 1AA"}))
(def destination (location/location [2.2 30.3] {:name "City"}))

(deftest test-add-start-end-locs
  (testing "Add the name/postcodes from input locations to the route output"
    (let [route-with-change [#::journey{:start-time (t/date-time 2021 1 1 9)
                                        :end-time (t/date-time 2021 1 1 9 10)
                                        :transport-type "walk"
                                        :start-location (location/location [1.1 20.2])
                                        :end-location (location/location [2.1 30.1])}

                             #::journey{:start-time (t/date-time 2021 1 1 9 10)
                                        :end-time (t/date-time 2021 1 1 9 20)
                                        :transport-type "train"
                                        :start-location (location/location [2.1 30.1])
                                        :end-location (location/location [2.2 30.3])}]
          route-without-change [#::journey{:start-time (t/date-time 2021 1 1 9)
                                           :end-time (t/date-time 2021 1 1 9 10)
                                           :transport-type "taxi/drive"
                                           :start-location (location/location [1.1 20.2])
                                           :end-location (location/location [2.2 30.3])}]]
      (testing "For a route with two steps"
        (let [result (sut/add-start-end-locs home destination route-with-change)]
          (is (= home
                 (journey/start-location result)))
          (is (= destination
                 (journey/end-location result)))))
      (testing "For a route with only one step"
        (let [result (sut/add-start-end-locs home destination route-without-change)]
          (is (= home
                 (journey/start-location result)))
          (is (= destination
                 (journey/end-location result))))))))
