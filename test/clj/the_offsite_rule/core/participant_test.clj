(ns the-offsite-rule.core.participant-test
  (:require [the-offsite-rule.core
             [participant :as sut]
             [location :as location]]
            [the-offsite-rule.io.mocks :as io]
            [clojure.test :refer :all]))


(def location-converter (io/->MockLocationConverter
                         {"W6 9DJ" [51.48984,-0.23186]}))

(deftest test-participant-created
  (let [result (sut/participant "mk"
                                "W6 9DJ"
                                location-converter)]
    (testing "Correct name"
      (is (= "mk"
             (::sut/name result))))
    (testing "Correct location"
      (is (= #::location{:postcode "W6 9DJ"
                         :coordinates #::location{:latitude 51.48984
                                                  :longitude -0.23186}}
             (::location/location result))))))
