(ns the-offsite-rule.api.api
  (:require [the-offsite-rule.api
             [user :as user]
             [event :as event]
             [search :as search]]
            [the-offsite-rule.io
             [db :as db]
             [postcodes :as postcodes]]
            [clj-time.coerce :as ct]
            [clojure.spec.alpha :as s]))

;;TODO: store user repositories between calls

;;;; temp for front end
;;(defn get-locations-for [params]
;;[{:total-time 70, :name "Wood Green", :journeys [{:name "mk's ghost", :travel-time 35} {:name "mk", :travel-time 35}]} {:total-time 82, :name "Camden Town", :journeys [{:name "mk's ghost", :travel-time 41} {:name "mk", :travel-time 41}]} {:total-time 92, :name "Islington", :journeys [{:name "mk's ghost", :travel-time 46} {:name "mk", :travel-time 46}]} {:total-time 94, :name "Hackney", :journeys [{:name "mk's ghost", :travel-time 47} {:name "mk", :travel-time 47}]} {:total-time 104, :name "Lambeth", :journeys [{:name "mk's ghost", :travel-time 52} {:name "mk", :travel-time 52}]} {:total-time 104, :name "City of Westminster", :journeys [{:name "mk's ghost", :travel-time 52} {:name "mk", :travel-time 52}]} {:total-time 110, :name "London", :journeys [{:name "mk's ghost", :travel-time 55} {:name "mk", :travel-time 55}]}]
;;)

(defn get-locations-for [user-repo {:keys [event-id]}]
    (user/event-locations user-repo event-id))


(defn get-event [user-repo {:keys [event-id]}]
  "Returns event participants as name/postcode map"
  (-> user-repo
      (user/event event-id)
      (update :time str)))

(defn get-events [user-repo]
  "Return a list of events for the user"
    (->> user-repo
         user/events
         (map (fn[e] (update e :time str)))));;TODO: split formatting part out to middleware

(defn- save-people [user-repo event-id people]
  (-> (user/maybe-update-people user-repo
                                event-id
                                people)
      (update :time str)))

(defn- run-model [user-repo event-id]
  (->> event-id
       (event/state user-repo)
       search/search-locations
       search/location-summaries
       (user/save-event-locations! user-repo event-id)))


;;TODO: save the whole event in event namespace, including name and time changes
;; TODO: don't do all this if participants are unchanged
(defn save-event-participants [user-repo {:keys [people event-id]}]
  "Saves participants an druns simulations, returning participants (if succesful) or error"
  (let [save-result (save-people user-repo
                                 event-id
                                 people)]
    (if (contains? save-result :error)
      save-result
      (run-model user-repo event-id))))

;;TODO should format time correctly
(defn new-event [user-repo {:keys [name time]}]
  "Create a new event with given name and time, returning the new event"
  (-> (user/new-event user-repo
                      (str name)
                      (ct/from-string time))
      (update :time str)))

(defn delete-event [user-repo {:keys [event-id]}]
  "Remove the event and return 200 if successful"
  (user/delete-event user-repo event-id))
