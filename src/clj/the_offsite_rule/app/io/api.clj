(ns the-offsite-rule.app.io.api
  (:require
   [the-offsite-rule.app.io.db :as db]
   [the-offsite-rule.app.io.map :as m]
   [the-offsite-rule.app.io.postcodes :as postcodes]
   [the-offsite-rule.app.io.format :as f]
   [the-offsite-rule.app.user :as user]
   [clj-time.core :as t]
   [the-offsite-rule.value :as value]))

(def postcode-converter (postcodes/->Converter))

(def map-api (m/->MapApi))

(defn list-events [{:keys [user-id]}]
  (let [repo (db/->DB user-id)]
    (->> user-id
        (db/->DB)
        (user/all-events)
        (map f/format-event-summary))))

(defn get-event [{:keys [user-id event-id]}]
  (-> user-id
      (db/->DB)
      (user/load-event event-id)
      f/format-event))

(defn delete-event [{:keys [user-id event-id]}]
  "return the element that was deleted or nil"
  (let [result (-> user-id
                   (db/->DB)
                   (user/delete-event event-id))]
       {:deleted (if result
                   event-id
                   nil)}))

(defn new-event [{:keys [user-id name time] :as data}]
  "Create the new event and return it"
      (-> user-id
          (db/->DB)
          (user/new-event (update data :time value/str->time))
          f/format-event))

(defn edit-event [{:keys [user-id event-id updates]}]
  "Returns the new event in user format"
    (-> user-id
      (db/->DB)
      (user/edit-event postcode-converter updates)
      f/format-event))

(defn run-event [{:keys [user-id event-id]}]
  "Runs, saves, and returns a list of location summaries"
  (-> user-id
      (db/->DB)
      (user/run-event-search map-api event-id)
      f/format-event-location-summaries))

;;TODO: this assumes that the event is already up to date! Check if it is and return error if not
;;TODO: we also need to check the location name is valid
(defn location-details [{:keys [user-id event-id location-name]}]
  "Return a list of route summaries for each participant"
  (-> user-id
      (db/->DB)
      (user/load-event event-id)
      (f/format-event-location-details location-name)))
