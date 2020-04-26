(ns the-offsite-rule.io.db
  (:require
   [the-offsite-rule.api.user :refer [EventGetter]]
   [clojure.java.jdbc :as j]
   [clj-time.coerce :as ct]
   [clojure.string :as string]))


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

(defn next-valid-id [id-name table-name]
  (let [rows (do (j/query db [(str "SELECT MAX(" id-name ") AS max FROM " table-name)]))]
    (-> rows
        first
        :max
        next-id))
  )
(defn- add-event-row [user-id event-id event-name event-time]
  (j/insert! db :event {:event_id event-id
                        :event_name event-name
                        :time (str event-time)
                        :user_id user-id}))

(defn- new-event [user-id event-name event-time]
  (let [event-id (next-valid-id "event_id" "event")]
    (add-event-row user-id
                   event-id
                   event-name
                   event-time)
    event-id))

(defn- fetch-event-details [event-id]
  (let [res (first (fetch-event-rows event-id))]
    (if (nil? res)
      res
      (assoc res :time (ct/from-string (:time res))))))

(defn- fetch-all-event-details [user-id]
  (let [res (fetch-user-events user-id)]
    (map (fn[row] (assoc row :time (ct/from-string (:time row))))
         res)))

(defn- fetch-location-rows [event-id]
  (j/query db ["SELECT * from event_location WHERE event_id = ?" event-id]))

(defn- delete-event-locations [event-id]
  (let [location-ids (->> event-id
                          fetch-location-rows
                          (map :id))]
    (do (j/delete! db :event_location ["event_id = ?" event-id])
        (if (> (count location-ids) 0)
          (j/execute! db [(str "DELETE FROM participant_route WHERE event_location_id IN ("
                               (string/join ", " location-ids)
                               ")")])))))


(defn- delete-event [event-id]
  (do (delete-event-locations [event-id])
      (j/delete! db :event ["event_id = ?" event-id])))


(defn- store-location [event-id location-name]
  (let [id (next-valid-id "id" "event_location")]
    (do (j/insert! db :event_location
                   {:id id
                    :event_id event-id
                    :location_name location-name})
        id)))

(defn- fetch-route-rows [event-location-id]
  (j/query db ["SELECT * from participant-route WHERE event_locaion_id = ?" event-location-id]))

(defn- store-route [location-id participant duration legs]
  (let [id (next-valid-id "id" "participant_route")]
    (j/insert! db
               :participant_route
               {:id id
                :event_location_id location-id
                :participant_name participant
                :journey_time_minutes duration
                :legs legs})))

(defrecord EventRepository [user-id]
  EventGetter
  (fetch-all-events [self] (fetch-all-event-details user-id))
  (fetch-event-participants [self event-id] (fetch-event-people event-id))
  (fetch-event [self event-id] (fetch-event-details event-id))
  (create-new-event [self event-name event-time] (new-event user-id event-name event-time))
  (update-event-people [self event-id people] (update-event-inputs people event-id))
  (delete-event [self event-id] (delete-event event-id))
  (user-id [self] user-id)
  (delete-all-event-locations [self event-id] (delete-event-locations event-id))
  (save-event-location [self event-id location-name] (store-location event-id location-name))
  (load-event-locations [self event-id] (fetch-location-rows event-id))
  (load-participant-routes [self event-location-id] (fetch-route-rows event-location-id))
  (save-participant-route [self event-location-id participant-name duration-minutes legs]
    (store-route event-location-id participant-name duration-minutes legs)))
