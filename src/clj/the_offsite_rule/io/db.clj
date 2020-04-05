(ns the-offsite-rule.io.db
  (:require
   [the-offsite-rule.api.user :refer [EventGetter]]
   [clojure.java.jdbc :as j]
   [clj-time.coerce :as ct]))

;;TODO: get rid of the result/error keyreturn and just return nil on error?

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "/home/mk-gsa/the-offsite-rule/the-offsite-rule"})


(defn- update-event-inputs [data event-id]
  (do (j/delete! db :person ["event_id = ?" event-id])  ;;TODO: don't do this if write fails
      (->> data
           (map #(assoc % :event_id event-id))
           (j/insert-multi! db :person)))
  data)

(defn- fetch-event-rows [event-id]
  (j/query db ["SELECT * FROM event WHERE event_id = ?" event-id]))

(defn- fetch-user-events [user-id]
  (j/query db ["SELECT event_id, datetime('event_name'), time FROM event WHERE user_id = ?" user-id]))

(defn- fetch-event-people [event-id]
  (j/query db ["SELECT name, postcode FROM person WHERE event_id = ?" event-id]))

(defn- next-id [current-max]
  (if (nil? current-max)
    0
    (inc current-max)))

(defn- next-valid-event-id []
  (let [rows (do (j/query db ["SELECT MAX(event_id) AS max FROM event"]))]
  (-> rows
      first
      :max
      next-id)))

(defn- add-event-row [user-id event-id event-name event-time]
  (j/insert! db :event {:event_id event-id
                        :event_name event-name
                        :time (str event-time)
                         :user_id user-id}))

(defn- new-event [user-id event-name event-time]
  (let [event-id (next-valid-event-id)]
    (add-event-row user-id
                   event-id
                   event-name
                   event-time)
    event-id))

(defn- fetch-event-time [event-id]
  (let [res (fetch-event-rows event-id)]
    (-> res
        first
        :time
        (ct/from-string))))

(defrecord EventRepository [user-id]
  EventGetter
  (fetch-all-event-ids [self] (fetch-user-events user-id))
  (fetch-event-participants-by-id [self event-id] (fetch-event-people event-id))
  (fetch-event-time [self event-id] (fetch-event-time event-id))
  (create-new-event [self event-name event-time] (new-event user-id event-name event-time))
  (update-event-people [self event-id people] (update-event-inputs people event-id)))
