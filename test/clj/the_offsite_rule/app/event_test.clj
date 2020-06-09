(ns the-offsite-rule.app.event-test
  (:require [the-offsite-rule.app.event :as sut]
            [the-offsite-rule
             [event :as e]
             [participant :as p]
             [location :as l]
             [journey :as j]
             [search :as search]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.test :refer :all]))

(deftest test-empty-event
  (let [result (sut/empty-event 0 "event" (t/date-time 2020 1 1 9))]
    (testing "Create event state for new event"
      (testing "State contains the correct event"
        (is (= #::e{:name "event"
                    :time (t/date-time 2020 1 1 9)
                    :participants []}
               (::e/event result))))
      (testing "Updated time matches current time to the second"
        (is (= (f/instant->map (t/now))
               (f/instant->map (::sut/last-update result)))))
      (testing "Last simulation time is nil"
        (is (nil? (::sut/last-simulation result))))
      (testing "Correct ID"
        (is (= 0
               (::sut/id result)))))))

(deftest test-update-participants
  (let [event-state {::e/event #::e{:name "event"
                                    :time (t/date-time 2020 1 1 9)
                                    :participants [#::p{:name "me"
                                                        :location #::l{:postcode "N4 3LR"
                                                                       :coordinates #::l{:latitude 1.1
                                                                                         :longitude 1.1}}}]}
                     ::sut/id 0
                     ::sut/last-update (t/date-time 2019 1 1 9)
                     ::sut/last-simulation nil}
        participants [#::p{:name "astronomer"
                           :location #::l{:postcode "SE10 8GX"
                                          :coordinates #::l{:latitude 0.0
                                                            :longitude 0.0}}}]
        result (sut/update-participants event-state participants)]
    (testing "New participants added and old ones removed"
      (is (= participants
             (get-in result [::e/event ::e/participants]))))
    (testing "Last update time is updated"
      (is (= (f/instant->map (t/now))
             (f/instant->map (::sut/last-update result)))))
    (testing "Last simulation time is unchanged"
      (is (nil? (::sut/last-simulation result))))))

(deftest test-update-time
  (let [event-state {::e/event #::e{:name "event"
                                    :time (t/date-time 2020 1 1 9)
                                    :participants [#::p{:name "me"
                                                        :location #::l{:postcode "N4 3LR"
                                                                       :coordinates #::l{:latitude 1.1
                                                                                         :longitude 1.1}}}]}
                     ::sut/id 0
                     ::sut/last-update (t/date-time 2019 1 1 9)
                     ::sut/last-simulation nil}
        result (sut/update-time event-state (t/date-time 2021 1 1 9))]

    (testing "Event time is updated"
      (is (= (t/date-time 2021 1 1 9)
             (get-in result [::e/event ::e/time]))))
    (testing "Update timestamp is updated"
      (is (= (f/instant->map (t/now))
             (f/instant->map (::sut/last-update result)))))))

(deftest test-update-name
  (let [event-state {::e/event #::e{:name "event"
                                    :time (t/date-time 2020 1 1 9)
                                    :participants [#::p{:name "me"
                                                        :location #::l{:postcode "N4 3LR"
                                                                       :coordinates #::l{:latitude 1.1
                                                                                         :longitude 1.1}}}]}
                     ::sut/id 0
                     ::sut/last-update (t/date-time 2019 1 1 9)
                     ::sut/last-simulation nil}]
    (testing "Updating event name changes only the name, not the update timestamp or other fields"
      (is (= (assoc-in event-state [::e/event ::e/name] "new name")
             (sut/update-name event-state "new name"))))))

(deftest test-up-to-date?
  (let [no-run-yet-state #::sut{:last-update (t/date-time 2020 1 1 9 30)
                                :last-simulation nil}
        changes-since-run-state #::sut{:last-update (t/date-time 2020 1 1 9 30)
                                       :last-simulation (t/date-time 2020 1 1 9 0)}
        up-to-date-state #::sut{:last-update (t/date-time 2020 1 1 9 30)
                                :last-simulation (t/date-time 2020 1 1 9 40)}]
    (testing "Event with no run time is not up to date"
      (is (false? (sut/up-to-date? no-run-yet-state))))
    (testing "Event with updates since last run is not up to date"
      (is (false? (sut/up-to-date? changes-since-run-state))))
    (testing "Event with run time after last update is up to date"
      (is (true? (sut/up-to-date? up-to-date-state))))))


;; Test-only map API

(def london (l/location [51.509865	-0.118092] {:name "London"}))

(defrecord SampleMap
    [location]
  search/Map
  (-locations [self] [location])
  (-route [self from to arrival-time] [{::j/start-time (t/minus arrival-time (t/minutes 10))
                                        ::j/end-time arrival-time
                                        ::j/transport-type "walk"
                                        ::j/start-location from
                                        ::j/end-location location}]))


(def sample-event
  #::e{:name "sample"
       :time (t/date-time 2020 12 1 10)
       :participants [(p/participant "MK"
                                     (l/location [51.5667 0.1178] {:postcode "N4 3LR"}))
                      (p/participant "milton keynes friend"
                                     (l/location [52.0436 0.7609] {:postcode "MK10 1SA"}))]})
(deftest test-run
  (let [event-already-run {::e/event sample-event
                           ::sut/id 0
                           ::sut/last-update (t/date-time 2020 1 1 9)
                           ::sut/last-simulation (t/date-time 2020 1 1 9 30)}
        event-to-run {::e/event sample-event
                      ::sut/id 0
                      ::sut/last-update (t/date-time 2020 1 1 9 30)
                      ::sut/last-simulation (t/date-time 2020 1 1 9)}]
    (testing "Event not updated since last run remains the same"
      (is (= event-already-run
             (sut/run event-already-run (->SampleMap london)))))
    (testing "Event that has been updated is re-run and the results added to the event"
      (let [result (sut/run event-to-run (->SampleMap london))]
        (testing "With new simulation timestamp"
          (is (= (f/instant->map (t/now))
                 (-> result
                     ::sut/last-simulation
                     f/instant->map))))
        (testing "With expected output locations"
          (is (= 1
                 (-> result
                     ::e/event
                     ::e/locations
                     count)))
          (is (= london
                 (-> result
                     ::e/event
                     ::e/locations
                     first
                     (select-keys [::l/coordinates
                                   ::l/name])))))
        (testing "With expected journey time"
          (is (= 20
                 (-> result
                     ::e/event
                     ::e/locations
                     first
                     ::e/total-journey-time
                     t/in-minutes))))))))
