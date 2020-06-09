(ns the-offsite-rule.event-test
  (:require [the-offsite-rule.event :as sut]
            [clj-time.core :as t]
            [clojure.test :refer :all]))

(deftest test-new-event
  (testing "Create an event without participants"
    (is (= #::sut{:name "my event"
                  :time (t/date-time 2020 1 1 9)
                  :participants []}
           (sut/event "my event" (t/date-time 2020 1 1 9))))))
