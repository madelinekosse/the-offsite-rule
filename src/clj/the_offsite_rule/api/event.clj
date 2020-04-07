(ns the-offsite-rule.api.event
  (:require [the-offsite-rule.io
             [cities :as cities]
             [routes :as routes]
             [postcodes :as postcodes]]
            [the-offsite-rule.core
             [event :as event]
             [participant :as participant]
             [location :as location]]))

(defprotocol EventGetter
  (fetch-all-events [self] "Return all event names and IDs for the user")
  (fetch-event-participants [self event-id] "Return the event participants with this ID")
  (fetch-event [self event-id] "Return the time for event as datetime, event name, event id")
  (create-new-event [self event-name event-time] "Return the ID of created event")
  (update-event-people [self event-id people] "Save people as this event"))


(defn- empty-event [{:keys [event_id event_name time]}]
  "Creates a new event with no participants"
  (event/event event_id event_name time []))

(defn- default-state [event-repository]
  {:event-repository event-repository
   :city-finder (cities/->Finder)
   :route-finder (routes/->Finder)})

(defn- person->participant [converter {:keys [name postcode]}]
  (participant/participant name postcode converter))

(defn- add-people [{:keys [event] :as state} people]
  (let [postcodes (map :postcode people)
        converter (postcodes/converter postcodes)
        participants (map (partial person->participant converter) people)]
    (-> state
        (assoc-in [:event ::event/participants] participants)
        (assoc :location-converter converter))))

(defn- load-participants [{:keys [event-repository event] :as state}]
  (let [people (fetch-event-participants event-repository (::event/id event))]
    (add-people state people)))

(defn load-event-state [event-id event-repository]
  {:pre [(satisfies? EventGetter event-repository)]}
  (->> event-id
       (fetch-event event-repository)
       empty-event
       (assoc (default-state event-repository) :event)
       load-participants))

(defn new-event! [name time repository]
  "creates a new event and returns state"
  (let [event-id (create-new-event repository name time)]
    (merge (default-state repository)
           {:event (empty-event event-id name time)})))

;;everything ffrom here on should operate on event state only

(defn add-participants [state people]
  (add-people state people))

(defn get-people [event-state]
  (map (fn [p] {:name (::participant/name p)
                :postcode (get-in p [::location/location ::location/postcode])})
       (get-in event-state [:event ::event/participants])))

;;TODO: this should also do name and time
(defn save-event [{:keys [event event-repository] :as state}]
  (let [people (get-people state)
        id (::event/id event)]
    (update-event-people event-repository id people)))

(defn load-all-events [event-repository]
  (fetch-all-events event-repository))
