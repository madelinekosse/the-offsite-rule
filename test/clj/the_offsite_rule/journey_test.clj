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

(def one-step-journey [#::sut{:start-time (t/date-time 2020 1 1 9)
                              :end-time (t/date-time 2020 1 1 9 15)
                              :transport-type "walk"
                              :start-location (l/location [51.1 0.1234])
                              :end-location (l/location [51.1 0.2345])}])

(deftest test-total-time
  (testing "Calculates the total journey time for list of legs"
    (testing "For journey with multiple step"
      (let [res (sut/total-time sample-journey)]
        (is (= 32
               (t/in-minutes res)))))
    (testing "For journey with one step"
      (let [res (sut/total-time one-step-journey)]
        (is (= 15
               (t/in-minutes res)))))))

(deftest test-num-changes
  (testing "Calculate number of changes for a journey"
    (testing "For journey with changes"
      (is (= 2
             (sut/num-changes sample-journey))))
    (testing "For journey with no changes"
      (is (= 0
             (sut/num-changes one-step-journey))))))

(deftest test-start-location
  (testing "Calculate the start location of the journey"
    (testing "For journey with multiple steps"
      (is (= (l/location [51.1 0.1234])
             (sut/start-location sample-journey))))
    (testing "For journey with one step"
      (is (= (l/location [51.1 0.1234])
             (sut/start-location one-step-journey))))))

(deftest test-end-location
  (testing "Calculate the end location of the journey"
    (testing "For journey with multiple steps"
      (is (= (l/location [51.1 0.4567])
             (sut/end-location sample-journey))))
    (testing "For journey with one step"
      (is (= (l/location [51.1 0.2345])
             (sut/end-location one-step-journey))))))
