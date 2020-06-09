(ns the-offsite-rule.event
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule
             [location :as l]
             [participant :as p]
             [journey :as j]
             [value :as value]]))

(s/def ::participants (s/coll-of ::p/participant))

(s/def ::time #(s/valid? ::value/time %))

(s/def ::name #(s/valid? ::value/name %))

(s/def ::total-journey-time #(s/valid? ::value/duration %))

;; should check everyone has a valid route

(s/def ::routes (s/coll-of ::j/journey))

(s/def ::location-meta (s/keys :req [::total-journey-time
                                     ::routes]))

(s/def ::location (s/merge ::l/location
                           ::location-meta))

(s/def ::locations (s/coll-of ::location))

(s/def ::event (s/keys :req [::name
                             ::time
                             ::participants]
                       :opt [::locations]))

(defn event
  ([name time] {::name name
                ::time time
                ::participants []})
  ([name time participants] {::name name
                             ::time time
                             ::participants participants}))

(defn participant-locations [event]
  {:pre [(s/valid? ::event event)]
   :post [(every? (fn[e] (s/valid? ::l/location e)) %)]}
  "Return a list of locations for all participants"
  (->> event
       ::participants
       (map ::p/location)))
