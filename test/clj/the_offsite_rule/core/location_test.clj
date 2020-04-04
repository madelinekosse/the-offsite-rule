(ns the-offsite-rule.core.location-test
  (:require [the-offsite-rule.core.location :refer [LocationConverter] :as sut]
            [the-offsite-rule.io.mocks :as io]
            [clojure.test :refer :all]
            [the-offsite-rule.core.location :as location]))


(def sample-postcode-map
  {"N4 3LR" [51.56712 -0.11773]})

(def converter (io/->MockLocationConverter sample-postcode-map))

(satisfies? LocationConverter converter)

(deftest test-new-from-coordinates
  (let [expected {::sut/coordinates {::sut/latitude 90.0
                                     ::sut/longitude 0.0}}]
    (testing "Created location from coordinates input"
      (is (= expected
             (sut/from-coordinates 90 0))))))

(deftest test-new-from-postcode
  (let [result (sut/from-postcode "N4 3LR" converter)]
    (testing "Coordinates added using converter"
      (is (= {::sut/latitude 51.56712
              ::sut/longitude -0.11773}
             (::sut/coordinates result))))
    (testing "Postcode is included"
      (is (= "N4 3LR"
             (::sut/postcode result))))))

(deftest test-center
  (let [inputs [(sut/from-coordinates -10 0)
                (sut/from-coordinates 10 20)]]
    (testing "Midpoint of two locations accurately calculated"
      (is (= {::sut/latitude 0.0
              ::sut/longitude 10.0}
             (sut/center inputs))))))

(deftest test-distance
  (let [location (sut/from-coordinates 0 0)
        coordinates {::sut/latitude 3.0
                     ::sut/longitude 4.0}]
    (testing "Correct pythagorean distance between two points"
      (is (= (sut/distance location coordinates)
             5.0)))))
