(ns the-offsite-rule.api.api
  (:require [the-offsite-rule.api
             [event :as event]
             [search :as search]]
            [the-offsite-rule.io
             [db :as db]
             [postcodes :as postcodes]]))

;;TODO: stop hard coding this and pass through
(def user-1-repo (db/->EventRepository 1))

;;WORKS
(defn get-locations-for [{:keys [event-id user-id]}]
  "returns a sorted list of event-locations"
  (let [state (event/load-event-state event-id user-1-repo)]
    (search/search-locations state)))

;;WORKS
(defn get-location-summary [state]
  (-> state
      get-locations-for
      search/list-best-location-times))

;;WORKS
(defn get-event-participants [{:keys [event-id user-id]}]
  "Returns event participants as name/postcode map"
  (let [state (event/load-event-state event-id user-1-repo)]
    (event/get-people state)))

;;WORKS
(defn get-events [{:keys [user-id]}]
  "Return a list of events for the user"
  (event/load-all-events user-1-repo))


;;WORKS
;;TODO: save the whole event in event namespace, including name and time changes
(defn save-event-participants [{:keys [people event-id user-id]}]
  "update event with new participants, returning nil or error map"
  (let [state (event/load-event-state event-id user-1-repo)
        new-state (event/add-participants state people)
        ]
    (-> state
        (event/add-participants people)
        event/save-event)))

;;WORKS
(defn new-event [{:keys [name time user-id]}]
  "Create a new event with given name and time, returning the new event ID"
  (event/new-event! name time user-1-repo))
