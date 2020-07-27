(ns the-offsite-rule.app.user
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.app.event :as event]
            [the-offsite-rule.app.participants :as ps]
            [the-offsite-rule.value :as value]))

(s/def ::id #(s/valid? ::value/id %))


;;TODO: these will be created for ser when authenticated in layer above
;; TODO: how will we handle database errors??
(defprotocol EventRepository
  (-user-id [_] "return the user id associated with this repository")
  (-all-events [_] "return all events for the user")
  (-next-event-id [_] "return the next available event ID")
  (-load-event [_ event-id] "return an event state for the id given")
  (-save-event [_ event-state] "save the event state and return it")
  (-delete-event [_ event-id] "delete the event given by the ID and return true on success"))

(defn- user-id [user-repo]
  {:pre [(satisfies? EventRepository user-repo)]
   :post [(s/valid? ::id %)]}
  (-user-id user-repo))

(defn all-events [user-repo]
  {:pre [(satisfies? EventRepository user-repo)]
   :post [(every? (fn[x] (s/valid? ::event/state x)) %)]}
  (->> user-repo
      -all-events
      (filter (fn [e] (t/before? (t/now) (event/time e))))
      (sort-by event/time)))

(defn- next-event-id [user-repo]
  {:pre [(satisfies? EventRepository user-repo)]
   :post [(s/valid? ::event/id %)]}
  (-next-event-id user-repo))

(defn- save-event [user-repo event-state]
  {:pre [(satisfies? EventRepository user-repo)
         (s/valid? ::event/state event-state)]
   :post [(= % event-state)]}
  (-save-event user-repo event-state))

(defn load-event [user-repo event-id]
  {:pre [(satisfies? EventRepository user-repo)
         (s/valid? ::event/id event-id)]
   :post [(s/valid? ::event/state %)]}
  (-load-event user-repo event-id))

(defn new-event [user-repo {:keys [name time]}]
  "Create and store a new event with given name and time"
  (let [event (-> user-repo
                  next-event-id
                  (event/empty-event name time))]
    (save-event user-repo event)))

(defn delete-event [user-repo event-id]
  {:pre [(satisfies? EventRepository user-repo)
         (s/valid? ::event/id event-id)]
   :post [(boolean? %)]}
  (-delete-event user-repo event-id))

(defn- maybe-update-name-time [event {:keys [name time]}]
  (cond-> event
    (some? name) (event/update-name name)
    (some? time) (event/update-time time)))

;; TODO: more tests, spec in/output, refactor
;;could use some consistency in how these namespaces are imported
;; also the order of arguments - interfaces maybe shouldnt go gits
(defn edit-event
  "Update the fields given in details using the io impementations given. postcode converter is neede to update participants"
  ([user-repo {:keys [id name time] :as details}]
   (let [updated-event (-> user-repo
                           (load-event id)
                           (maybe-update-name-time details))]
     (save-event user-repo updated-event)))
  ([user-repo postcode-converter {:keys [id name time participants] :as details}]
   (let [event (load-event user-repo id)
         updated-event (if (some? participants)
                         (-> event
                             (maybe-update-name-time details)
                             (event/update-participants (ps/participants participants postcode-converter)))
                         (maybe-update-name-time event details))]
     (save-event user-repo updated-event))))

;;TODO: threads like these are ugly because we don't have a consistent order. either interfaces or state goes first with options as a map at the end - should work everywhere!

(defn run-event-search [user-repo map-api event-id]
  "Loads the event, runs it if needed and saves the updated version, returning the result also"
  (let [updated-event (-> user-repo
                         (load-event event-id)
                         (event/run map-api))]
    (save-event user-repo updated-event)))
