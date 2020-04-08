(ns the-offsite-rule.core.event
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core
             [location :as location]
             [participant :as participant]]))

(s/def ::participants (s/coll-of ::participant/participant))

(s/def ::time inst?)

(s/def ::name string?)

(s/def ::id int?)

(s/def ::event (s/keys :req [::participants
                             ::time
                             ::id
                             ::name]))

(defn event [id name time participants]
  {:pre [(s/valid? inst? time)
         (s/valid? ::participants participants)]
   :post [(s/valid? ::event %)]}
  {::time time
   ::participants participants
   ::name name
   ::id id})

(defn midpoint [event]
  {:pre [(s/valid? ::event event)]
   :post [(s/valid? ::location/coordinates %)]}
  (->> event
       ::participants
       (map ::location/location)
       (location/center)))
