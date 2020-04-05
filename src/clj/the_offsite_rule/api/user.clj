(ns the-offsite-rule.api.user
  (:require [the-offsite-rule.core
             [event :as event]
             [location :refer [LocationConverter] :as location]]
            [clojure.spec.alpha :as s]))

;; TODO: implement seperate users
;;TODO: this needs a time setter
(defprotocol EventGetter
  (fetch-all-event-ids [self] "Return all event names and IDs for the user")
  (fetch-event-participants-by-id [self event-id] "Return the event participants with this ID")
  (fetch-event-time [self event-id] "Return the time for event as datetime")
  (create-new-event [self event-name event-time] "Return the ID of created event")
  (update-event-people [self event-id people] "Save people as this event"))

(defn events [getter]
  {:pre [(satisfies? EventGetter getter)]}
  (fetch-all-event-ids getter))

(defn- make-participant [postcode-converter {:keys [name postcode]}]
  "From a :name :postcode pair and converter, return participant"
  (event/participant name postcode postcode-converter))

(defn event [event-id event-getter postcode-converter]
  {:pre [(satisfies? EventGetter event-getter)
         (satisfies? LocationConverter postcode-converter)]}
  (let [participants (fetch-event-participants-by-id event-getter event-id)
        time (fetch-event-time event-getter event-id)]
    (->> participants
         (map (partial make-participant postcode-converter))
         (event/event time))))

(defn- event->postcode-list [event]
  (->> event
       ::event/participants
       (map (fn [x] {:name (::event/name x)
                     :postcode (get-in x [::location/location
                                          ::location/postcode])}))))

(defn save-people [event-id event event-getter]
  {:pre [(s/valid? ::event/event event)]}
  (->> event
       event->postcode-list
       (update-event-people event-getter event-id)))
