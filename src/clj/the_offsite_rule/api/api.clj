(ns the-offsite-rule.api.api
  (:require
   [the-offsite-rule.db :as db]))


(defn get-locations-for [{:keys [event-id]}]
  "returns a sorted list of event-locations")

(defn get-event-participants [{:keys [event-id]}]
  "Returns een participants as name/postcode map")

(defn get-event-ids [{:keys [user-id]}]
  "Return a list of event IDs for the user")

(defn save-event-participants [{:keys [people event-id]}]
  "update event with new participants, returning nil or error map")

;;TODO add time to event db
(defn update-event-time [{:keys [time event-id]}]
  "Update the event date and time")

(defn new-event [{:keys [name time]}]
  "Create a new event with given name and time, returning the new event ID")

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
