(ns the-offsite-rule.search-test
  (:require [the-offsite-rule
             [search :as sut]
             [event :as e]
             [journey :as j]
             [participant :as p]
             [location :as l]]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [clojure.spec.alpha :as s]))

(def sample-locations [(l/location [51.752022	-1.257677] {:name "Oxford"})
                       (l/location [51.509865	-0.118092] {:name "London"})
                       (l/location [51.8043 0.4401] {:name "Hemel Hampstead"})
                       (l/location [55.953251	-3.188267] {:name "Edinburgh"})
                       (l/location [51.481583	-3.179090] {:name "Cardiff"})
                       (l/location [56.3398 2.7967] {:name "St Andrews"})
                       (l/location [53.8008 1.5491] {:name "Leeds"})
                       (l/location [53.4808 2.2426] {:name "Manchester"})
                       ])

(defn make-route [from to arrival-time time-map]
  "Pass in a map of the duration of journey from to to and return a sample route with that length"
  (let [postcode (::l/postcode from)
        city (::l/name to)
        time (get-in time-map [postcode city])
        duration (t/minutes time)
        intermediate-location (-> from
                                  (cons [to])
                                  l/midpoint
                                  (assoc ::l/name "intermediate station"))]
    [{::j/start-time (t/minus arrival-time duration)
      ::j/end-time (t/minus arrival-time (t/minutes 1))
      ::j/transport-type "train"
      ::j/start-location from
      ::j/end-location intermediate-location}

     {::j/start-time (t/minus arrival-time (t/minutes 1))
      ::j/end-time arrival-time
      ::j/transport-type "walk"
      ::j/start-location intermediate-location
      ::j/end-location to}]))

(defrecord SampleMap
    [travel-times]
  sut/Map
  (-locations [self] sample-locations)
  (-route [self from to arrival-time] (make-route from to arrival-time travel-times)))


(def sample-event
  #::e{:name "sample"
       :time (t/date-time 2020 12 1 10)
       :participants [(p/participant "MK"
                                     (l/location [51.5667 0.1178] {:postcode "N4 3LR"}))
                      (p/participant "milton keynes friend"
                                     (l/location [52.0436 0.7609] {:postcode "MK10 1SA"}))]})


(deftest test-best-locations
  (let [sample-travel-times {"N4 3LR" {"Hemel Hampstead" 60
                                       "London" 15
                                       "Oxford" 80
                                       "Cardiff" 200
                                       "Edinburgh" 300
                                       "Leeds" 300
                                       "Manchester" 300
                                       "St Andrews" 300
                                       }
                             "MK10 1SA" {"Hemel Hampstead" 20
                                         "London" 45
                                         "Oxford" 40
                                         "Cardiff" 150
                                         "Edinburgh" 260
                                         "Leeds" 300
                                         "Manchester" 300
                                         "St Andrews" 300}}
        result (sut/best-locations sample-event (->SampleMap sample-travel-times))]
    (testing "London is shortest route for both, though hemel hampstead is geographically closer"
      (is (= "London"
             (-> result
                 first
                 ::l/name))))
    (testing "Total travel time to London is 1 hour"
      (is (= 1
             (-> result
                 first
                 ::e/total-journey-time
                 (t/in-hours)))))))

(deftest test-best-locations-truncated
  (let [sample-travel-times {"N4 3LR" {"Hemel Hampstead" 10
                                       "London" 15
                                       "Oxford" 80
                                       "Cardiff" 200
                                       "Edinburgh" 300
                                       "Leeds" 300
                                       "Manchester" 300
                                       "St Andrews" 10
                                       }
                             "MK10 1SA" {"Hemel Hampstead" 20
                                         "London" 45
                                         "Oxford" 40
                                         "Cardiff" 150
                                         "Edinburgh" 260
                                         "Leeds" 300
                                         "Manchester" 300
                                         "St Andrews" 10}}
        result (sut/best-locations sample-event (->SampleMap sample-travel-times))]
    (testing "St Andrews route is not found - search returns when improvement converges"
      (is (= "Hemel Hampstead"
             (-> result
                 first
                 ::l/name))))
    (testing "Search terminates after 5 failed searches and doesn't return result for most distant locations"
      (is (= 6
             (count result))))))
