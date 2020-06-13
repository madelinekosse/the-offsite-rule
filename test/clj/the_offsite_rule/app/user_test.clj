(ns the-offsite-rule.app.user-test
  (:require [the-offsite-rule.app.user :as sut]
            [the-offsite-rule.app.event :as state]
            [the-offsite-rule.app.mock-io :as mocks]
            [the-offsite-rule
             [event :as e]
             [participant :as p]
             [location :as l]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.test :refer :all]))

(deftest test-new-event
  (testing "Creates a new event"
    (let [db (mocks/->MockDB (atom []))
          result (sut/new-event db
                                {:name "my event"
                                 :time (t/date-time 2021 1 1 9)})]
      (testing "With correct name"
        (is (= "my event"
               (get-in result [::e/event ::e/name]))))
      (testing "With correct time"
        (is (= (t/date-time 2021 1 1 9)
               (get-in result [::e/event ::e/time]))))
      (testing "With updated time set to now"
        (is (= (f/instant->map (t/now))
               (-> result
                   ::state/last-update
                   f/instant->map))))
      (testing "With nil as last simulation time"
        (is (nil? (::state/last-simulation result))))
      (testing "With next ID given by database"
        (is (= 0
               (::state/id result))))
      (testing "saves the new event and reloads it"
        (is (= result
               (sut/load-event db 0)))))))

(deftest test-edit-event
  (let [db (mocks/->MockDB (atom []))
        old-event (sut/new-event db
                                 {:name "my event"
                                  :time (t/date-time 2021 1 1 9)})]
    (testing "Edit the event name and time"
      (let [result (sut/edit-event db
                                   {:id 0
                                    :name "new name"
                                    :time (t/date-time 2021 1 2 9)})]
        (testing "event is saved"
          (is (= result
                 (sut/load-event db 0))))
        (testing "Returns the updated event state"
          (is (= {::e/name "new name"
                  ::e/time (t/date-time 2021 1 2 9)
                  ::e/participants []}
                 (::e/event result))))
        (testing "Updates the last updated timestamp"
          (is (= (f/instant->map (t/now))
                 (-> result
                     ::state/last-update
                     f/instant->map))))))
    (testing "Edit event participants"
      (let [result (sut/edit-event
                    db
                    (mocks/->MockConverter)
                    {:id 0
                     :participants [{:name "Alice" :postcode "NG7 2QL"}]})]
        (testing "Adds the new participants location using converter"
          (is (= mocks/alices-house
                 (-> result
                     (get-in [::e/event ::e/participants])
                     first
                     ::p/location))))
        (testing "Removes the old participants"
          (let [new-participants (->> {:id 0
                                       :participants [{:name "Eduardo" :postcode "N4 3LR"}]}
                                      (sut/edit-event db (mocks/->MockConverter))
                                      ::e/event
                                      ::e/participants)]
            (is (= mocks/eduardos-house
                   (::p/location (first new-participants))))
            (is (= 1
                   (count new-participants)))))))))

(deftest test-run-event
  (let [db (mocks/->MockDB (atom []))
        old-event (sut/new-event db
                                 {:name "my event"
                                  :time (t/date-time 2021 1 1 9)})]
    (testing "Running event with empty participants returns the same event"
      (let [result (sut/run-event-search db (mocks/->MockMap) 0)]
        (is (= old-event
               result))))
    (testing "Run event with participants"
      (let [_ (sut/edit-event
               db
               (mocks/->MockConverter)
               {:id 0
                :participants [{:name "Eduardo" :postcode "N4 3LR"}
                               {:name "Alice" :postcode "NG7 2QL"}]})
            result (sut/run-event-search db (mocks/->MockMap) 0)
            ;;_ (temp/save-data 0 result)
            ]
        (testing "saves the output"
          (is (= result
                 (sut/load-event db 0))))
        (testing "returns best location"
          (let [best (-> result
                         ::e/event
                         ::e/locations
                         first)]
            (is (= "London"
                   (::l/name best)))
            (is (= 138
                   (t/in-minutes (::e/total-journey-time best))))
            (is (= 2
                   (count (::e/routes best))))))
        (testing "updates last simulation timestamp"
          (is (= (f/instant->map (t/now))
                 (f/instant->map (::state/last-simulation result)))))))))

(deftest test-delete-event
  (testing "Delete event"
    (let [db (mocks/->MockDB (atom []))
        event-to-delete (sut/new-event db
                                       {:name "my event"
                                        :time (t/date-time 2021 1 1 9)})
        event-to-keep (sut/new-event db
                                       {:name "my event to keep"
                                        :time (t/date-time 2021 1 1 9)})
        delete-result (sut/delete-event db 0)
        remaining-events (sut/all-events db)]
      (testing "Removes the event given"
        (is (= 1
               (count remaining-events))))
      (testing "Preserves IDs of remaining events"
        (is (= 1
               (-> remaining-events
                   first
                   ::state/id))))
      (testing "Returns true"
        (is (true? delete-result)))
      )))
