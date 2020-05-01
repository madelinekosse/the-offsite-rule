(ns the-offsite-rule.api.api
  (:require [the-offsite-rule.api
             [user :as user]
             [event :as event]
             [search :as search]]
            [the-offsite-rule.io
             [db :as db]
             [postcodes :as postcodes]]
            [clj-time.coerce :as ct]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]))

;; maps event id to run results
(def past-run-results (atom {}))

;;TODO: store user repositories between calls

(defn- format-times [event]
  (let [maybe-stringify (fn[m k]
                          (if (contains? m k)
                            (update m k str)
                            m))]
    (-> event
        (maybe-stringify :time)
        (maybe-stringify :last-simulation)
        (maybe-stringify :last-update))))

(defn get-locations-for [user-repo {:keys [event-id]}]
  "Returns three keys: last-update, last-simulation, and locations"
  (let [last-result (get @past-run-results event-id)
        latest-result (if (or (nil? last-result)
                              (nil? (:last-simulation last-result))
                              (t/before? (:last-simulation last-result) (:last-update last-result)))
                        (user/event-locations user-repo event-id)
                        last-result)]
    (format-times latest-result)))

(defn get-event [user-repo {:keys [event-id]}]
  "Returns event participants as name/postcode map"
  (-> user-repo
      (user/event event-id)
      format-times))

(defn get-events [user-repo]
  "Return a list of events for the user"
    (->> user-repo
         user/events
         (map format-times)));;TODO: split formatting part out to middleware

(defn- save-people [user-repo event-id people]
  (-> (user/maybe-update-people user-repo
                                event-id
                                people)
      format-times))

(defn- run-model [user-repo event-id]
  (let [results (->> event-id
                     (event/state user-repo)
                     search/search-locations
                     search/location-summaries
                     (user/save-event-locations! user-repo event-id))]
    (swap! past-run-results #(assoc % event-id results))))

;;TODO: save the whole event in event namespace, including name and time changes
;; TODO: don't do all this if participants are unchanged
(defn save-event-participants [user-repo {:keys [people event-id]}]
  "Saves participants and triggers simulation, returning participants (if succesful) or error"
  (let [save-result (save-people user-repo
                                 event-id
                                 people)]
    (if (contains? save-result :error)
      save-result
      (let [f (future (run-model user-repo event-id))]
        save-result))))

;;TODO should format time correctly
(defn new-event [user-repo {:keys [name time]}]
  "Create a new event with given name and time, returning the new event"
  (-> (user/new-event user-repo
                      (str name)
                      (ct/from-string time))
      (update :time str)))

(defn delete-event [user-repo {:keys [event-id]}]
  "Remove the event and return 200 if successful"
  (user/maybe-delete-event user-repo event-id))
