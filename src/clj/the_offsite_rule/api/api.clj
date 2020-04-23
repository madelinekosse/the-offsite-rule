(ns the-offsite-rule.api.api
  (:require [the-offsite-rule.api
             [user :as user]
             [event :as event]
             [search :as search]]
            [the-offsite-rule.io
             [db :as db]
             [postcodes :as postcodes]]
            [clj-time.coerce :as ct]))

;;TODO: store user repositories between calls

(defn get-locations-for [{:keys [event-id user-id]}]
  "returns a sorted list of event-locations"
  (let [user-repo (db/->EventRepository user-id)
        state (event/state user-repo event-id)]
    (search/search-locations state)))


(defn get-event [{:keys [event-id user-id]}]
  "Returns event participants as name/postcode map"
  (let [user-repo (db/->EventRepository user-id)]
    (-> user-repo
        (user/event event-id)
        (update :time str)
        )))

(defn get-events [{:keys [user-id]}]
  "Return a list of events for the user"
  (let [user-repo (db/->EventRepository user-id)]
    (->> user-repo
         user/events
         (map (fn[e] (update e :time str))))));;TODO: split formatting part out to middleware


;;TODO: save the whole event in event namespace, including name and time changes
(defn save-event-participants [{:keys [people event-id user-id]}]
  "update event with new participants, returning nil or error map"
  (let [user-repo (db/->EventRepository user-id)]
    (-> (user/maybe-update-people user-repo
                                  event-id
                                  people)
        (update :time str))))

;;TODO should format time correctly
(defn new-event [{:keys [name time user-id]}]
  "Create a new event with given name and time, returning the new event"
  (let [user-repo (db/->EventRepository user-id)]
    (-> (user/new-event user-repo
                        (str name)
                        (ct/from-string time))
        (update :time str))))

(defn delete-event [{:keys [event-id user-id]}]
  "Remove the event and return 200 if successful"
  (let [user-repo (db/->EventRepository user-id)]
    (user/delete-event user-repo event-id)))
