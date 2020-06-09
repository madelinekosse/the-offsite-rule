(ns the-offsite-rule.app.io.format
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.app.event :as event-state]
            [the-offsite-rule.app.participants :as participants]
            [the-offsite-rule.app.user :as user]
            [the-offsite-rule
             [event :as event]
             [value :as value]
             [location :as location]
             [participant :as p]]

            [the-offsite-rule.event :as e]
            [the-offsite-rule.location :as l]))

;; namespace to define API output formats

;; string time format
(s/def ::time #(s/valid? ::value/time-str %))

(s/def ::duration #(s/valid? ::value/duration-map %))

;; whether the search has been run since last update
(s/def ::up-to-date? boolean?)

(s/def ::participants (s/every ::participants/name-postcode-pair))

(s/def ::event-summary (s/keys :req-un [::event-state/id
                                        ::event/name
                                        ::time
                                        ::up-to-date?]
                               :opt-un [::participants]))

(s/def ::location-summary (s/keys :req-un [::location/name
                                           ::duration]))


(defn format-event-summary [summary]
  {:pre [(s/valid? ::user/event-meta summary)]
   :post [(s/valid? ::event-summary %)]}
  "Convert event metadata to correct output format"
  {:id (::event-state/id summary)
   :name (::event/name summary)
   :time (-> summary
             ::event/time
             value/time->str)
   :up-to-date? (event-state/up-to-date? summary)})

;;TODO: refactor to avoid repetition
;;I'd love it if we could move some functions down and ONLY call the layer above not the core

(defn format-event [e]
  {:pre [(s/valid? ::event-state/state e)]
   :post [(s/valid? ::event-summary %)]}
  "Format a full event state for the front end input page with meta and participants"
  (let [meta {
              :id (::event-state/id e)
              :name (get-in e [::event/event ::event/name])
              :time (-> e ::event/event ::event/time value/time->str)
              :up-to-date? (event-state/up-to-date? e)
              }]
    (->> e
         ::event/event
         ::event/participants
         (map participants/name-and-postcode)
         (assoc meta :participants))))

(defn format-event-location-summaries [e]
   {:pre [(s/valid? ::event-state/state e)]
    :post [every? (fn[l] (s/valid? ::location-summary l)) %]}
  (let [location->summary (fn[l] {:name (::l/name l)
                                  :duration (value/duration->map (::event/total-journey-time l))})]
    (->> e
         ::event/event
         ::event/locations
         (map location->summary))))

(defn- valid-or-nil? [spec value]
  "True if the value is missing or conforms to spec"
  (if (some? value)
    (s/valid? spec value)
    true))

(defn params-error? [{:keys [user-id event-id name time participants] :as params}]
  "Returns an error string or nil if no error"
  (let [validation-status {:user-id (valid-or-nil? ::user/id user-id)
                           :event-id (valid-or-nil? ::event-state/id event-id)
                           :name (valid-or-nil? ::event/name name)
                           :time (valid-or-nil? ::event/time time)
                           :participants (valid-or-nil? ::participants participants)}]
    (if (->> validation-status
             vals
             (every? true?))
      nil
      (->> validation-status
           (filter (fn[[k v]] (false? v)))
           keys
           (map (fn[k] (vector (clojure.core/name k) (get params k))))
           (map #(str (first %) ": " (last %) " "))
           (apply str)
           (str "Bad input for: ")))))
