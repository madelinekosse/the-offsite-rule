(ns the-offsite-rule.db
  (:require
   [clojure.java.jdbc :as j]
   [clojure.pprint :as p]))


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

(defn- fetch-people-rows [event-id]
  (j/query db ["SELECT * FROM person WHERE event_id = ?" event-id]))

;; this could also look at the user id
(defn valid-event? [event-id]
  (try-catch-return-exception
   (fn[id] (-> event-id
               fetch-event-rows
               empty?
               not))
   event-id))

(defn store-input-locations [inputs event-id]
  (try-catch-return-exception -update-event-inputs inputs event-id))

(defn fetch-event-inputs [event-id]
  (try-catch-return-exception fetch-people-rows event-id))
