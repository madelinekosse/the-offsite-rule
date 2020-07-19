(ns the-offsite-rule.app.io.format
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.app.event :as event]
            [the-offsite-rule.app.participants :as participants]
            [the-offsite-rule.app.user :as user]
            [the-offsite-rule.app.location :as location]
            [the-offsite-rule
             [value :as value]
             [journey :as j]]
            [clojure.string :as string]))

;; namespace to define API output formats

;; string time format
(s/def ::time #(s/valid? ::value/time-str %))

(s/def ::duration #(s/valid? ::value/duration-map %))

;; whether the search has been run since last update
(s/def ::up-to-date? boolean?)

(s/def ::participants (s/every ::participants/name-postcode-pair))

(s/def ::event-summary (s/keys :req-un [::value/id
                                        ::value/name
                                        ::time
                                        ::up-to-date?]
                               :opt-un [::participants]))

(s/def ::location-summary (s/keys :req-un [::value/name
                                           ::duration]))


(s/def ::changes int?)

(s/def ::route-summary (s/keys :req-un [::value/name
                                        ::duration
                                        ::changes]))

;;TODO: refactor to avoid repetition
;;I'd love it if we could move some functions down and ONLY call the layer above not the core

(defn format-event [e]
  {:pre [(s/valid? ::event/state e)]
   :post [(s/valid? ::event-summary %)]}
  "Format a full event state for the front end input page with meta and participants"
  {:id (::event/id e)
   :name (event/name e)
   :time (-> e
             event/time
             value/time->str)
   :up-to-date? (event/up-to-date? e)
   :participants (->> e
                      event/participants
                      (map participants/name-and-postcode))})

(defn format-event-location-summaries [e]
  {:pre [(s/valid? ::event/state e)]
   :post [every? (fn[l] (s/valid? ::location-summary l)) %]}
  (let [location->summary (fn[loc] {:name (location/name loc)
                                    :duration (value/duration->map (location/total-journey-time loc))})]
    (->> e
         event/locations
         (map location->summary))))

(defn- route-for-participant  [participant routes]
  "return the route that matches participants start location"
  (->> routes
       (filter (fn[r] (= (j/start-location r) (participants/location-for participant))))
       first))

;; TODO: what if we have 2 with same name?
(defn format-event-location-details [e location-name]
  {:pre [(s/valid? ::event/state e)]
   :post [(every? (fn[x] (s/valid? ::route-summary x)) %)]}
  (let [participants (event/participants e)
        routes (->> e
                    event/locations
                    (filter (fn[l] (= location-name (location/name l))))
                    first
                    location/routes)]
    (->> participants
         (map (fn[p] (assoc p :route (route-for-participant p routes))))
         (map (fn[p] {:name (participants/name-for p)
                      :duration (-> p
                                    :route
                                    j/total-time
                                    value/duration->map)
                      :changes (j/num-changes (:route p))})))))

(defn- valid-or-nil? [spec value]
  "True if the value is missing or conforms to spec"
  (if (some? value)
    (s/valid? spec value)
    true))

(defn- split-postcode [postcode]
  (let [len (count (string/split postcode #""))
        outbound (subs postcode 0 (- len 3))
        inbound (subs postcode (- len 3))]
    (str (string/upper-case outbound)
         " "
         (string/upper-case inbound))))

(defn- parse-postcode [postcode]
  (let [parts (string/split postcode #" ")]
    (if (= 1 (count parts))
      (split-postcode postcode)
      (string/upper-case postcode))))

(defn- format-participant-update [update-map]
  (if (contains? update-map :participants)
    (->> update-map
         :participants
         (map (fn[p] (update p :postcode parse-postcode)))
         (assoc update-map :participants))
    update-map))

(defn- format-time-update [update-map]
  (if (contains? update-map :time)
    (update update-map :time value/str->time)
    update-map))

(defn- parse-updates [event-id update-data]
  "Parses the update fields from request into format used for logic"
  (let [updates (assoc update-data :id event-id)]
    (-> updates
        format-time-update
        format-participant-update)))

(defn parse-request-body [{:keys [event-id] :as body}]
  (-> body
      (update :updates (partial parse-updates event-id))
      format-time-update))

;;TODO: split it into params error and body (ie upate) error and test the above. This is far to complicated when we could have separate one for params and body
;; This is the only place we use the core event namespace - remove it or move this somewhere else so we only use app/event
(defn params-error? [{:keys [updates] :as params}]
  "Returns an error string or nil if no error"
  (let [validation-items (merge params updates)
        ;; replace name/time fields with updates is ok since it'll never happen that we get both
        validation-status {:user-id (valid-or-nil? ::user/id (:user-id validation-items))
                           :event-id (valid-or-nil? ::event/id (:event-id validation-items))
                           :name (valid-or-nil? ::value/name (:name validation-items))
                           :time (valid-or-nil? ::value/time-str (:time validation-items))
                           :participants (valid-or-nil? ::participants (:participants validation-items))
                           :location-name (valid-or-nil? ::value/name (:location-name validation-items))}]
    (if (->> validation-status
             vals
             (every? true?))
      nil
      (->> validation-status
           (filter (fn[[k v]] (false? v)))
           keys
           (map (fn[k] (vector (clojure.core/name k) (get validation-items k))))
           (map #(str (first %) ": " (last %)))
           (string/join ", ")
           (str "Bad input for: ")))))
