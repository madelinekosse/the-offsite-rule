(ns the-offsite-rule.app.participants-test
  (:require [the-offsite-rule.app.participants :as sut]
            [the-offsite-rule
             [location :as l]
             [participant :as p]]
            [clojure.test :refer :all]))


(deftest test-new-participant
  (testing "Creates a participant conforming to participant spec"
    (is (= #::p{:name "test-name"
                :location #::l{:coordinates #::l{:latitude (float 0.51)
                                                 :longitude (float 51.0)}
                               :postcode "N4 3LR"}}
           (sut/new-participant "test-name" "N4 3LR" 0.51 51)))))

(defrecord postcode-converter [lookup]
  sut/PostcodeConverter
  (-postcode-lookup [self postcodes] lookup))

(deftest test-participants
  (let [inputs [{:name "Bert"
                 :postcode "W6 9DJ"}
                {:name "Ernie"
                 :postcode "E3 3BD"}]
        lookup {"W6 9DJ" {:latitude 0.3
                          :longitude 50.0}
                "E3 3BD" {:latitude 0.1
                          :longitude 51.0}}
        expected [#::p{:name "Bert"
                       :location #::l{:coordinates #::l{
                                                        :latitude (float 0.3)
                                                        :longitude (float 50.0)
                                                        }
                                      :postcode "W6 9DJ"}}
                  #::p{:name "Ernie"
                       :location #::l{:coordinates #::l{:latitude (float 0.1)
                                                        :longitude (float 51.0)}
                                      :postcode "E3 3BD"}}]]
    (testing "Multiple participants created with postcode converter"
      (is (= expected
             (sut/participants inputs (->postcode-converter lookup)))))))
