(ns the-offsite-rule.api.user
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core
             [event :as event]
             [participant :as participant]
             [location :as location]]))

(defprotocol EventGetter
  (fetch-all-events [self] "Return all event names and IDs for the user")
  (fetch-event-participants [self event-id] "Return the event participants with this ID")
  (fetch-event [self event-id] "Return the time for event as datetime, event name, event id")
  (create-new-event [self event-name event-time] "Return the ID of created event")
  (update-event-people [self event-id people] "Save people as this event")
  (delete-event [self event-id] "Delete the event and return the row deleted")
  (user-id [self] "Return the user ID"))

(s/def ::event-meta (s/keys :req-un [::event/name
                                     ::event/id
                                     ::event/time]))

(s/def ::participant-input (s/keys :req-un [::participant/name
                                            ::location/postcode]))

(s/def ::event-participants (s/coll-of ::participant-input))

(s/def ::event-input (s/merge ::event-meta
                              (s/keys :req-un [::event-participants])))

(s/def ::events (s/coll-of ::event-meta))

(defn- event-row->event-input [{:keys [event_id event_name time]}]
  {:name event_name
   :id event_id
   :time time})

(defn event [event-repo event-id]
  {:pre [(satisfies? EventGetter event-repo)]
   :post [(s/valid? ::event-input %)]}
  "Returns an event details as given"
  (let [event-row (fetch-event event-repo event-id)
        participants (fetch-event-participants event-repo event-id)]
    (merge
     (event-row->event-input event-row)
     {:event-participants participants})))

(defn events [event-repo]
  {:pre [(satisfies? EventGetter event-repo)]
   :post [(s/valid? ::events %)]}
  "Returns a list of event-metss without participants"
  (->> event-repo
       fetch-all-events
       (map event-row->event-input)))

(defn new-event [event-repo event-name event-time]
  {:pre [(satisfies? EventGetter event-repo)
         (s/valid? ::event/name event-name)
         (s/valid? ::event/time event-time)]
   :post [(s/valid? ::event-input %)]}
  "return created event with no participants and including ID"
  (let [event-id (create-new-event
                  event-repo
                  event-name
                  event-time)]
    {:name event-name
     :id event-id
     :time event-time
     :event-participants []}))

(defn- update-people [event-repo event-id people]
  {:pre [(satisfies? EventGetter event-repo)
         (s/valid? ::event-participants people)]
   :post [(s/valid? ::event-input %)]}
  "Save and return updated event"
  (let [saved-people (update-event-people event-repo
                                          event-id
                                          people)
        event-details (event event-repo event-id)]
    (assoc event-details :event-participants saved-people)))

(defn maybe-update-people [event-repo event-id people]
  (if (s/valid? ::event-participants people)
    (update-people event-repo event-id people)
    {:error "invalid input"}))

(defn delete-event [event-repo event-id]
  {:pre [(satisfies? EventGetter event-repo)
         (s/valid? ::event/id event-id)]
   :post [(or (= (:deleted %) event-id)
              (contains? % :error))]}
  (let [event (fetch-event event-repo event-id)]
    (if (or (nil? event) (not= (:user_id event) (user-id event-repo)))
      {:error "No such event for user"}
      (try
        (do (delete-event event-repo event-id)
            (update-event-people event-repo event-id [])
            {:deleted event-id})
        (catch Throwable e {:error (.toString e)})))))
