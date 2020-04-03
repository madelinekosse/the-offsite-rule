(ns the-offsite-rule.core.location-test
  (:require [the-offsite-rule.core.location :refer [LocationConverter] :as sut]
            [clojure.test :refer :all]))

(defrecord MockLocationConverter [lat lng]
  LocationConverter
  (postcode-coordinates [this postcode] {::sut/latitude lat ::sut/longitude lng}))

(def zero-converter (->MockLocationConverter 0.0 0.0))

(deftest test-new-from-coordinates
  (let [expected {::sut/coordinates {::sut/latitude 90.0
                                     ::sut/longitude 0.0}}]
    (testing "Created location from coordinates input"
      (is (= expected
             (sut/from-coordinates 90 0))))))

(deftest test-new-from-postcode
  (let [expected (assoc
                  (sut/from-coordinates 0 0)
                  ::sut/postcode
                  "N4 3LR")]
    (testing "Coordinates added using converter"
      (is (= expected
             (sut/from-postcode "N4 3LR" zero-converter))))))

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
