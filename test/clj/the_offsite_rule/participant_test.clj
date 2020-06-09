(ns the-offsite-rule.participant-test
  (:require [the-offsite-rule.participant :as sut]
            [the-offsite-rule.location :as l]
            [clojure.test :refer :all]))

(deftest test-participant
  (let [loc (l/location [51 1] {:postcode "N4 3LR"})]
    (testing "Constructs participant with given name and location"
      (is (= #::sut{:name "MK"
                    :location loc}
             (sut/participant "MK" loc))))))
