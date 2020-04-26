(ns the-offsite-rule.api.search
  (:require [the-offsite-rule.core
             [search :as search]
             [event-location :as event-location]
             [event :as event]
             [location :as location]
             [participant :as participant]
             [journey :as journey]]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core.leg :as leg]
            ))

;; This spec tells us how we want to interact with the DB and front end
(s/def ::location-name string?)
(s/def ::duration int?)
(s/def ::from-loc #(or (string? %) (nil? %)))
(s/def ::to-loc #(or (string? %) (nil? %)))
(s/def ::leg (s/keys :req-un [::leg/transport-type
                              ::duration
                              ::from-loc
                              ::to-loc]))
(s/def ::journey (s/coll-of ::leg))

(s/def ::participant-route (s/keys :req-un [::participant/name
                                            ::journey
                                            ::duration]))
(s/def ::routes (s/coll-of ::participant-route))
(s/def ::location-summary (s/keys :req-un [::location-name
                                           ::duration
                                           ::routes]))
(defn search-locations [{:keys [event city-finder route-finder postcode-converter] :as state}]
  "Add a list of event locations to the state"
  (assoc state :locations (search/best-locations event city-finder route-finder)))

(defn- leg-summary [leg]
  {:pre (s/valid? ::leg/leg leg)
   :post (s/valid? ::leg %)}
  (let [duration (t/in-minutes (t/interval (::leg/start-time leg) (::leg/end-time leg)))]
    {:transport-type (::leg/transport-type leg)
     :from-loc (-> leg
                   ::leg/start-location
                   ::location/name)
     :to-loc (-> leg
                 ::leg/end-location
                 ::location/name)
     :duration duration}))

(defn- journey-summary [participant]
  {:pre [(s/valid? ::participant/participant participant)]
   :post [(s/valid? ::participant-route %)]}
  "Return participant route"
  {:name (::participant/name participant)
   :duration (journey/total-time-minutes (::journey/route participant))
   :journey (map leg-summary (::journey/route participant))})

(defn- location-summary [candidate]
  {:pre [(s/valid? ::event-location/event candidate)]
   :post [(s/valid? ::location-summary %)]}
  {:duration (::event-location/total-travel-minutes candidate)
   :location-name (get-in candidate [::location/location ::location/name])
   :routes (map journey-summary (::event/participants candidate))})

(defn location-summaries [{:keys [locations] :as state}]
  (map location-summary locations))
