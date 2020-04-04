(ns the-offsite-rule.core.event-test
  (:require [the-offsite-rule.core.event :as sut]
            [the-offsite-rule.io.mocks :as io]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [the-offsite-rule.core.location :as location]
            [clojure.spec.alpha :as s]))

(def sample-postcode-map
  {"N4 3LR" [51.56712 -0.11773]
   "E3 3BD" [51.52880 -0.01409]
   "W6 9DJ" [51.48984,-0.23186]})

(def location-converter (io/->MockLocationConverter sample-postcode-map))

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

(def sample-participants
  [(sut/participant "MK" "N4 3LR" location-converter)
   (sut/participant "DK" "W6 9DJ" location-converter)])

(deftest test-event-midpoint
  (let [event (sut/event (t/date-time 2020 1 1 10 0)
                          sample-participants)]
    (testing "correct midpoint of participants"
      (is (= #::location{:latitude 51.52848
                         :longitude -0.174795}
             (sut/midpoint event))))))
