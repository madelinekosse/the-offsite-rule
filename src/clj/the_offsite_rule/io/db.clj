(ns the-offsite-rule.io.db
  (:require
   [the-offsite-rule.api.user :refer [EventGetter]]
   [clojure.java.jdbc :as j]
   [clj-time.core :as t]))

;;TODO: get rid of the result/error keyreturn and just return nil on error?

(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "/home/mk-gsa/the-offsite-rule/the-offsite-rule"})


(defn- try-catch-return-exception [f & args]
  (try {:result (apply f args)}
       (catch Exception e
         {:error (.getMessage e)})))

(defn- -update-event-inputs [data event-id]
  (do (j/delete! db :person ["event_id = ?" event-id])  ;;TODO: don't do this if write fails
      (->> data
           (map #(assoc % :event_id event-id))
           (j/insert-multi! db :person)))
  data)

(defn- fetch-event-rows [event-id]
  (j/query db ["SELECT * FROM event WHERE event_id = ?" event-id]))

(defn- fetch-user-event-rows [user-id]
  (j/query db ["SELECT event_id, event_name FROM event WHERE user_id = ?" user-id]))

(defn- fetch-people-rows [event-id]
  (j/query db ["SELECT name, postcode FROM person WHERE event_id = ?" event-id]))

;; this could also look at the user id
;;(defn valid-event? [event-id]
  ;;(try-catch-return-exception
   ;;(fn[id] (-> event-id
               ;;fetch-event-rows
               ;;empty?
               ;;not))
   ;;event-id))

(defn- store-input-locations [inputs event-id]
  (try-catch-return-exception -update-event-inputs inputs event-id))

(defn- fetch-event-inputs [event-id]
  (try-catch-return-exception fetch-people-rows event-id))

(defn- fetch-user-events [user-id]
  (try-catch-return-exception fetch-user-event-rows user-id))

(defrecord EventRepository [user-id]
  EventGetter
  (fetch-all-event-ids [self] (fetch-user-events user-id))
  (fetch-event-participants-by-id [self event-id] (fetch-event-inputs event-id))
  (fetch-event-time [self event-id] (t/date-time 2021 1 1 9 30)) ;;TODO implement in db
  (update-event-people [self event-id people] (store-input-locations people event-id)))
