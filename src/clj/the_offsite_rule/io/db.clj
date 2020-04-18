(ns the-offsite-rule.io.db
  (:require
   [the-offsite-rule.api.user :refer [EventGetter]]
   [clojure.java.jdbc :as j]
   [clj-time.coerce :as ct]))


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
  (j/query db ["SELECT event_id, event_name, time FROM event WHERE user_id = ?" user-id]))

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

(defn- fetch-event-details [event-id]
  (let [res (first (fetch-event-rows event-id))]
    (assoc res :time (ct/from-string (:time res)))))

(defn- fetch-all-event-details [user-id]
  (let [res (fetch-user-events user-id)]
    (map (fn[row] (assoc row :time (ct/from-string (:time row))))
         res)))

(defrecord EventRepository [user-id]
  EventGetter
  (fetch-all-events [self] (fetch-all-event-details user-id))
  (fetch-event-participants [self event-id] (fetch-event-people event-id))
  (fetch-event [self event-id] (fetch-event-details event-id))
  (create-new-event [self event-name event-time] (new-event user-id event-name event-time))
  (update-event-people [self event-id people] (update-event-inputs people event-id)))
