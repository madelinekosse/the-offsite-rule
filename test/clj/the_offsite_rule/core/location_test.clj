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
      (is (= (sut/from-coordinates 90 0)
             expected)))))

(deftest test-new-from-postcode
  (let [expected (assoc
                  (sut/from-coordinates 0 0)
                  ::sut/postcode
                  "N4 3LR")]
    (testing "Coordinates added using converter"
      (is (= (sut/from-postcode "N4 3LR" zero-converter)
             expected)))))
