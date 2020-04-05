(ns the-offsite-rule.api.api
  (:require [the-offsite-rule.api
             [event :as event]
             [user :as user]]
            [the-offsite-rule.io
             [db :as db]
             [postcodes :as postcodes]]
            [clj-time.core :as t])) ;;testing only

;;TODO: stop hard coding this and pass through
(def user-1-repo (db/->EventRepository 1))


(defn get-locations-for [{:keys [event-id user-id]}]
  "returns a sorted list of event-locations")

(defn get-event-participants [{:keys [event-id user-id]}]
  "Returns event participants as name/postcode map")

(defn get-event-ids [{:keys [user-id]}]
  "Return a list of event IDs for the user")

;;TODO: save the whole event in event namespace, minimise user ns

(defn save-event-participants [{:keys [people event-id user-id]}]
  "update event with new participants, returning nil or error map"
  (let [state (event/event-state event-id user-1-repo)
        new-event (event/update-participants people state)]
    (event/save state user-1-repo)))

;;TODO add backend for this or do it as part of the save
(defn update-event-time [{:keys [time event-id user-id]}]
  "Update the event date and time")

;; is it ok that these two functions don't create an event?
(defn new-event [{:keys [name time user-id]}]
  "Create a new event with given name and time, returning the new event ID"
  (let [event-repo (db/->EventRepository user-id)]
    (user/new-event name time event-repo)))

;;TODO: server and client seperate errors
;;TODO: move all of this out somewhere else (its db specific)

;;(def connection-error-response {:error "Could not connect to database"})
;;
;;(defn- maybe-convert-server-error [potential-error]
  ;;(if (:error potential-error)
      ;;{:error "Server error"}
      ;;potential-error))
;;
;;(defn- people-error [people] nil) ;;TODO: make this actually work
;;
;;(defn- event-error [event-id]
  ;;"return either nil or an error map"
  ;;(let [validation (db/valid-event? event-id)]
    ;;(cond
      ;;(nil? event-id) {:error "No event ID"}
      ;;(:error validation) connection-error-response
      ;;(false? (:result validation)) {:error "No such event"}
      ;;:else nil)))
;;
;;(defn- validate-inputs [{:keys [people event-id] :as data}]
  ;;(let [event-error? (event-error event-id)
        ;;people-error? (people-error people)]
    ;;(cond
      ;;(some? event-error?) event-error?
      ;;(some? people-error?) people-error?
      ;;:else data)))
;;
;;(defn save-event-data! [{:keys [people event-id] :as data}]
  ;;(let [validated-data (validate-inputs data)]
    ;;(if (= validated-data data)
      ;;(maybe-convert-server-error (db/store-input-locations people event-id))
      ;;validated-data)))
;;
;;(defn get-event-data [{:keys [event-id]}]
  ;;(let [error (event-error event-id)]
    ;;(if (some? error)
      ;;error
      ;;(maybe-convert-server-error (db/fetch-event-inputs event-id)))))
;;
;;this should be initialised with user id eventually
