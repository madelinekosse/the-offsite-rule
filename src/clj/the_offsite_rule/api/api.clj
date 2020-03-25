(ns the-offsite-rule.api.api
  (:require
   [the-offsite-rule.db :as db]))

;;TODO: server and client seperate errors

(def connection-error-response {:error "Could not connect to database"})

(defn- maybe-convert-server-error [potential-error]
  (if (:error potential-error)
      {:error "Server error"}
      potential-error))

(defn- people-error [people] nil) ;;TODO: make this actually work

(defn- event-error [event-id]
  "return either nil or an error map"
  (let [validation (db/valid-event? event-id)]
    (cond
      (nil? event-id) {:error "No event ID"}
      (:error validation) connection-error-response
      (false? (:result validation)) {:error "No such event"}
      :else nil)))

(defn- validate-inputs [{:keys [people event-id] :as data}]
  (let [event-error? (event-error event-id)
        people-error? (people-error people)]
    (cond
      (some? event-error?) event-error?
      (some? people-error?) people-error?
      :else data)))

(defn save-event-data! [{:keys [people event-id] :as data}]
  (let [validated-data (validate-inputs data)]
    (if (= validated-data data)
      (maybe-convert-server-error (db/store-input-locations people event-id))
      validated-data)))

(defn get-event-data [{:keys [event-id]}]
  (let [error (event-error event-id)]
    (if (some? error)
      error
      (maybe-convert-server-error (db/fetch-event-inputs event-id)))))
