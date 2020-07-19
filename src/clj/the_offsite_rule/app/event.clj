(ns the-offsite-rule.app.event
  (:require
   [clojure.spec.alpha :as s]
   [clj-time.core :as t]
   [the-offsite-rule
    [event :as e]
    [search :as search]
    [value :as value]]))

(s/def ::last-update #(s/valid? ::value/time %))
(s/def ::last-simulation #(or (nil? %) (s/valid? ::value/time %)))
(s/def ::id #(s/valid? ::value/id %))

(s/def ::state (s/keys :req [::last-update
                             ::last-simulation
                             ::id
                             ::e/event]))

(s/def ::summary (s/keys :req [::id
                               ::last-simulation
                               ::last-update
                               ::e/name
                               ::e/time]))

(defn empty-event [id name time]
  {:pre [(s/valid? ::id id)
         (s/valid? ::value/name name)
         (s/valid? ::value/time time)]
   :post [(s/valid? ::state %)]}
  "Return a new state for the created event"
  {::last-update (t/now)
   ::last-simulation nil
   ::id id
   ::e/event (e/event name time)})

(defn- mark-update [event-state]
  (assoc event-state ::last-update (t/now)))

;;TODO: if we just changed the names of people, don't update last update time
(defn update-participants [event-state participants]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::state %)]}
  "Update the event state with new participants, removing the old ones and updating timestamp"
  (-> event-state
      (assoc-in [::e/event ::e/participants] participants)
      mark-update))

(defn update-time [event-state new-time]
  {:pre [(s/valid? ::state event-state)
         (s/valid? ::value/time new-time)]
   :post [(s/valid? ::state %)]}
  "Modify the event time and return state with new update timestamp"
  (-> event-state
      (assoc-in [::e/event ::e/time] new-time)
      mark-update))

(defn update-name [event-state new-name]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::state %)]}
  "Update the event name without changing the update time"
  (assoc-in event-state [::e/event ::e/name] new-name))

(defn up-to-date? [event-state]
  "Return true if the search has run since the event was last changed"
  (cond
    (nil? (::last-simulation event-state)) false
    (t/before? (::last-simulation event-state) (::last-update event-state)) false
    :else true))

(defn run [event-state map-api]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::state %)]}
  "Run the search and update locations in event if needed"
  (if (or (up-to-date? event-state)
          (empty? (get-in event-state [::e/event ::e/participants])))
    event-state
    (let [results (search/best-locations (::e/event event-state)
                                         map-api)]
      (-> event-state
          (assoc-in [::e/event ::e/locations] results)
          (assoc ::last-simulation (t/now))))))

(defn name [event-state]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::e/name %)]}
  "Returns the event name"
  (get-in event-state [::e/event ::e/name]))

(defn time [event-state]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::e/time %)]}
  "Returns the event time"
  (get-in event-state [::e/event ::e/time]))

(defn id [event-state]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::id %)]}
  "Returns the event id"
  (::id event-state))

(defn participants [event-state]
  {:pre [(s/valid? ::state event-state)]
   :post [(s/valid? ::e/participants %)]}
  "Returns the event participants"
  (get-in event-state [::e/event ::e/participants]))

(defn locations [event-state]
  {:pre [(s/valid? ::state event-state)]
   :post [(or (nil? %) (s/valid? ::e/locations %))]}
  "Returns the event participants"
  (get-in event-state [::e/event ::e/locations]))
