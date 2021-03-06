;;TODO: create some files in resources containing test cases and load them in using db
;; this also means adding a constructor to the db thing for filename, but that would be useful
;; since we won't use it for anything but testing

(ns the-offsite-rule.app.io.format-test
  (:require [clojure.test :refer :all]
            [the-offsite-rule.app.io.format :as sut]
            [the-offsite-rule.app.event :as event-state]
            [the-offsite-rule.app.user :as user]
            [the-offsite-rule.app.participants :as ps]
            [the-offsite-rule.event :as event]
            [clj-time.core :as t]

            [the-offsite-rule.location :as l]
            [the-offsite-rule.journey :as j]
            [clojure.spec.alpha :as s]))

(deftest test-format-event
  (testing "Format event and participants for front end"
    (let [basic-event {::event-state/id 1
                       ::event-state/last-simulation (t/date-time 2020 1 1 9)
                       ::event-state/last-update (t/date-time 2020 1 1 9 30)
                       ::event/event (event/event "my event" (t/date-time 2021 1 1 9))}
          basic-expected {:id 1
                          :name "my event"
                          :time "2021-01-01T09:00:00.000Z"
                          :up-to-date? false
                          :participants []}]
      (testing "For event with no participants"
        (is (= basic-expected
               (sut/format-event basic-event))))
      (testing "For event with participants"
        (is (= (assoc basic-expected
                      :participants
                      [{:name "mk" :postcode "N4 3LR"}
                       {:name "dk" :postcode "W6 9DJ"}])
               (-> basic-event
                   (assoc-in [::event/event ::event/participants]
                             [(ps/new-participant "mk" "N4 3LR" 0 0)
                              (ps/new-participant "dk" "W6 9DJ" 0.3 51.0)])
                   sut/format-event)))))))

(deftest test-format-event-location-summaries
  (testing "Format the locations of an event"
    (let [raw-event (event/event
                     "my event"
                     (t/date-time 2021 1 1 9)
                     [(ps/new-participant "mk" "N4 3LR" 0 0) (ps/new-participant "dk" "W6 9DJ" 0.3 51.0)])
          location (-> (l/location [1 1] {:name "city"})
                       (assoc ::event/total-journey-time (t/minutes 100))
                       (assoc ::event/routes [
                                              [{::j/start-location (l/location [0 0] {:postcode "N4 3LR"})
                                                ::j/end-location (l/location [1 1] {:name "city"})
                                                ::j/transport-type "train"
                                                ::j/start-time (t/date-time 2021 1 1 8 10)
                                                ::j/end-time (t/date-time 2021 1 1 9)
                                                }]

                                              [{::j/start-location (l/location [0.3 51.0] {:postcode "W6 9DJ"})
                                                ::j/end-location (l/location [1 1] {:name "city"})
                                                ::j/transport-type "train"
                                                ::j/start-time (t/date-time 2021 1 1 8 10)
                                                ::j/end-time (t/date-time 2021 1 1 9)
                                                }]]))
          event-state-no-locations {::event-state/id 1
                                    ::event-state/last-simulation (t/date-time 2020 1 1 9 30)
                                    ::event-state/last-update (t/date-time 2020 1 1 9)
                                    ::event/event raw-event}
          event-state-with-locations (assoc-in event-state-no-locations
                                               [::event/event ::event/locations]
                                               [location])]
      (testing "For an event with no locations, returns empty list"
        (is (= []
               (sut/format-event-location-summaries event-state-no-locations))))
      (testing "For an event with locations, return a list of summaries"
        (is (= [{:name "city" :duration {:hours 1 :minutes 40 :seconds 0}}]
               (sut/format-event-location-summaries event-state-with-locations)))))))

(deftest test-format-event-location-details
  (testing "Format each participants route to a given location"
    (let [raw-event (event/event
                     "my event"
                     (t/date-time 2021 1 1 9)
                     [(ps/new-participant "mk" "N4 3LR" 0 0) (ps/new-participant "dk" "W6 9DJ" 0.3 51.0)])
          location (-> (l/location [1 1] {:name "city"})
                       (assoc ::event/total-journey-time (t/minutes 100))
                       (assoc ::event/routes [
                                              [{::j/start-location (l/location [0 0] {:postcode "N4 3LR"})
                                                ::j/end-location (l/location [1 1] {:name "city"})
                                                ::j/transport-type "train"
                                                ::j/start-time (t/date-time 2021 1 1 8 10)
                                                ::j/end-time (t/date-time 2021 1 1 9)
                                                }]

                                              [{::j/start-location (l/location [0.3 51.0] {:postcode "W6 9DJ"})
                                                ::j/end-location (l/location [1 1] {:name "city"})
                                                ::j/transport-type "train"
                                                ::j/start-time (t/date-time 2021 1 1 8 15)
                                                ::j/end-time (t/date-time 2021 1 1 9)
                                                }]]))
          event-state-no-locations {::event-state/id 1
                                    ::event-state/last-simulation (t/date-time 2020 1 1 9 30)
                                    ::event-state/last-update (t/date-time 2020 1 1 9)
                                    ::event/event raw-event}
          event-state-with-locations (assoc-in event-state-no-locations
                                               [::event/event ::event/locations]
                                               [location])]
      (is (= [{:name "mk" :duration {:hours 0 :minutes 50 :seconds 0} :changes 0}
              {:name "dk" :duration {:hours 0 :minutes 45 :seconds 0} :changes 0}]
             (sut/format-event-location-details event-state-with-locations "city"))))))

(deftest test-params-error
  (testing "Validation of API parameters"
    (testing "For IDs used to access event"
      (testing "Returns nil if no errors found"
        (is (nil? (sut/params-error? {:user-id 0
                                      :event-id 0}))))
      (testing "Returns error string for all invalid parameters"
        (is (= "Bad input for: user-id: -1, event-id: not an id"
               (sut/params-error? {:user-id -1
                                   :event-id "not an id"})))))
    (testing "For new event name and time"
      (testing "Returns nil if parameters are valid"
        (is (nil? (sut/params-error? {:event-id 0
                                      :name "my event"
                                      :time "2021-01-01T09:00:00.000Z"}))))
      (testing "Returns error string for invalid time format"
        (is (= "Bad input for: time: 20210101T090000"
               (sut/params-error? {:event-id 0
                                   :name "my event"
                                   :time "20210101T090000"})))))
    (testing "For updates to event"
      (testing "Returns nil if all fields are given and valid"
        (is (nil? (sut/params-error? {:updates {:name "my event"
                                                :time "2021-01-01T09:00:00.000Z"
                                                :participants []}}))))
      (testing "Returns nil if some fields are empty"
        (is (nil? (sut/params-error? {:updates {:name "my event"}}))))
      (testing "Returns error string if participants are not valid"
        (is (= "Bad input for: participants: [{:name \"mk\", :postcode \"12345\"}]"
               (sut/params-error? {:updates {:participants [{:name "mk" :postcode "12345"}]}}))))
      (testing "Error string includes errors to nested updates"
        (is (= "Bad input for: name: 100"
               (sut/params-error? {:updates {:name 100}})))))
    (testing "For location name parameter"
      (testing "Only strings are accepted"
        (is (nil? (sut/params-error? {:location-name "London"})))
        (is (= "Bad input for: location-name: London"
               (sut/params-error? {:location-name 'London})))))))

(deftest test-parse-request-body
  (testing "Formats event update input data for processing"
    (let [result (sut/parse-request-body {:event-id 0
                                          :updates {:time "2020-01-01T11:11:11.000Z"
                                                    :name "new event name"
                                                    :participants [{:name "maddy and emily" :postcode "n43lr"}
                                                                   {:name "alice" :postcode "w6 9dj"}]}})]
      (testing "Timestamp is converted to datetime"
        (is (= (t/date-time 2020 1 1 11 11 11)
               (get-in result [:updates :time]))))
      (testing "Event ID is added to the update map"
        (is (= 0
               (get-in result [:updates :id]))))
      (testing "Participant postcodes are converted to upper case"
        (is (= "W6 9DJ"
               (-> result
                   :updates
                   :participants
                   last
                   :postcode))))
      (testing "Participant postcodes given witout a space are separated into components"
        (is (= "N4 3LR"
               (-> result
                   :updates
                   :participants
                   first
                   :postcode)))))))

(deftest test-parse-updates
  (testing "nil"
    (is (= 0 0))))
