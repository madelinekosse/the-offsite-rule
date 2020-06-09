(ns the-offsite-rule.location-test
  (:require [the-offsite-rule.location :as sut]
            [clojure.test :refer :all]))

(deftest test-constructor
  (testing "Create location with postcode"
    (is (= #::sut{:coordinates #::sut{:latitude 90.0
                                      :longitude 0.0}
                  :postcode "N4 3LR"}
           (sut/location [90 0] {:postcode "N4 3LR"}))))
  (testing "Create location with name"
    (is (= #::sut{:coordinates #::sut{:latitude (float 51.1)
                                      :longitude (float 0.1234)}
                  :name "home"}
           (sut/location [51.1 0.1234] {:name "home"})))))

(deftest test-midpoint
  (let [inputs [(sut/location [-10 0])
                (sut/location [10 20])]
        expected #::sut{:coordinates #::sut{:latitude 0.0
                                            :longitude 10.0}}]
    (testing "Midpoint of two locations accurately calculated"
      (is (= expected
             (sut/midpoint inputs))))))

(deftest test-distance
  (let [l1 (sut/location [0 0])
        l2 (sut/location [3 4])]
    (testing "Correct pythagorean distance between two points"
      (is (= (sut/distance l1 l2)
             5.0)))))
