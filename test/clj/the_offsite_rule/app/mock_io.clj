(ns the-offsite-rule.app.mock-io
  (:require [the-offsite-rule.app.user :as user]
            [the-offsite-rule.app.event :as event]
            [the-offsite-rule.app.participants :refer [PostcodeConverter]]
            [the-offsite-rule
             [event :as e]
             [search :as s]
             [journey :as j]
             [location :as l]]
            [clj-time.core :as t]))

(defn- event->summary [event]
  (merge (select-keys event
                      [::event/id
                       ::event/last-simulation
                       ::event/last-update])
         (select-keys (::e/event event) [::e/name ::e/time]))
  )

;;events atom is an indexed list of event states
(defrecord MockDB [events-atom]
  user/EventRepository
  (-user-id [self] 0)
  (-all-events [self] (->> @events-atom
                           (filter some?)
                           (map event->summary)
                           (map (fn[e] (assoc e ::user/id 0)))))
  (-next-event-id [self] (count @events-atom))
  (-load-event [self event-id] (nth @events-atom event-id))
  (-save-event [self event] (do (swap! events-atom
                                       (fn[events new-event]
                                         (let [id (::event/id new-event)]
                                           (if (= id (count events))
                                             (conj events new-event)
                                             (assoc events id new-event))))
                                       event)
                                event))
  (-delete-event [self event-id] (do (swap! events-atom (fn[events] (assoc events event-id nil)))
                                     true)))

(def london (l/location [51.514248 -0.093145] {:name "London"}))
(def birmingham (l/location [52.466667 -1.916667] {:name "Birmingham"}))
(def manchester (l/location [53.5 -2.216667] {:name "Manchester"}))
(def leeds (l/location [53.8 -1.583333] {:name "Leeds"}))
(def sheffield (l/location [53.366667 -1.5] {:name "Sheffield"}))
(def glasgow (l/location [55.833333 -4.25] {:name "Glasgow"}))
(def newcastle (l/location [54.988056 -1.619444] {:name "Newcastle Upon Tyne"}))
(def nottingham (l/location [52.966667 -1.166667] {:name "Nottingham"}))
(def liverpool (l/location [53.416667 -3.0] {:name "Liverpool"}))

(def cities [london
             birmingham
             manchester
             leeds
             sheffield
             glasgow
             newcastle
             nottingham
             liverpool])

(def postcode-lookup-helper {"NG7 2QL" [52.936191  -1.205367]
                             "N4 3LR" [51.5667 0.1178]
                             "W6 9DJ" [51.489932 -0.231865]
                             "IP1 1AY" [52.0561 1.154311]
                             "NP7 1BA" [51.810216 -3.119627]})

(def alices-house (l/location (get postcode-lookup-helper "NG7 2QL") {:postcode "NG7 2QL"}))
(def eduardos-house (l/location (get postcode-lookup-helper "N4 3LR") {:postcode "N4 3LR"}))
(def davids-house (l/location (get postcode-lookup-helper "W6 9DJ") {:postcode "W6 9DJ"}))
(def savannahs-house (l/location (get postcode-lookup-helper "IP1 1AY") {:postcode "IP1 1AY"}))
(def jamies-house (l/location (get postcode-lookup-helper "NP7 1BA") {:postcode "NP7 1BA"}))

;; format is postcode from : city to: minutes
(def route-helper {"NG7 2QL" {"London" 116
                              "Birmingham" 120
                              "Manchester" 143
                              "Leeds" 155
                              "Sheffield" 83
                              "Glasgow" 423
                              "Newcastle Upon Tyne" 221
                              "Nottingham" 23
                              "Liverpool" 202}
                   "N4 3LR" {"London" 22
                             "Birmingham" 109
                             "Manchester" 154
                             "Leeds" 200
                             "Sheffield" 160
                             "Glasgow" 393
                             "Newcastle Upon Tyne" 216
                             "Nottingham" 146
                             "Liverpool" 194}
                   "W6 9DJ" {"London" 13
                             "Birmingham" 145
                             "Manchester" 181
                             "Leeds" 212
                             "Sheffield" 176
                             "Glasgow" 418
                             "Newcastle Upon Tyne" 196
                             "Nottingham" 156
                             "Liverpool" 212}
                   "IP1 1AY" {"London" 87
                              "Birmingham" 207
                              "Manchester" 257
                              "Leeds" 260
                              "Sheffield" 246
                              "Glasgow" 452
                              "Newcastle Upon Tyne" 288
                              "Nottingham" 222
                              "Liverpool" 280}
                   "NP7 1BA" {"London" 229
                              "Birmingham" 168
                              "Manchester" 177
                              "Leeds" 250
                              "Sheffield" 227
                              "Glasgow" 499
                              "Newcastle Upon Tyne" 354
                              "Nottingham" 247
                              "Liverpool" 179}})

(defrecord MockMap []
  s/Map
  (-locations [self] cities)
  (-route [self from to arrival-time]
    (let [time-mins (get-in route-helper [(::l/postcode from) (::l/name to)])]
      [#::j{:start-time (t/minus arrival-time (t/minutes time-mins))
            :end-time arrival-time
            :start-location from
            :end-location to
            :transport-type "train"}])))

(defrecord MockConverter []
  PostcodeConverter
  (-postcode-lookup [self postcodes] (->> postcodes
                                         (select-keys postcode-lookup-helper)
                                         (reduce-kv (fn [m k v]
                                                      (assoc m k {:latitude (first v)
                                                                  :longitude (last v)}))
                                                    {}))))
