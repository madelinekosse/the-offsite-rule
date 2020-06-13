(ns the-offsite-rule.journey-test
  (:require [the-offsite-rule
             [journey :as sut]
             [location :as l]]
            [clj-time.core :as t]
            [clojure.test :refer :all]))

(def sample-journey [#::sut{:start-time (t/date-time 2020 1 1 9)
                            :end-time (t/date-time 2020 1 1 9 15)
                            :transport-type "walk"
                            :start-location (l/location [51.1 0.1234])
                            :end-location (l/location [51.1 0.2345])}

                     #::sut{:start-time (t/date-time 2020 1 1 9 17)
                            :end-time (t/date-time 2020 1 1 9 30)
                            :transport-type "train"
                            :start-location (l/location [51.1 0.2345])
                            :end-location (l/location [51.1 0.3456])}

                     #::sut{:start-time (t/date-time 2020 1 1 9 30)
                            :end-time (t/date-time 2020 1 1 9 32)
                            :transport-type "walk"
                            :start-location (l/location [51.1 0.3456])
                            :end-location (l/location [51.1 0.4567])}])

(deftest test-total-time
  (testing "Calculates the total journey time for list of legs"
    (let [res (sut/total-time sample-journey)]
      (is (= 32
           (t/in-minutes res))))))

(deftest test-num-changes
  (testing "Calculate number of changes for a journey"
    (is (= 2
           (sut/num-changes sample-journey)))))

(deftest test-start-location
  (testing "Calculate the start location of the journey"
    (is (= (l/location [51.1 0.1234])
           (sut/start-location sample-journey)))))
