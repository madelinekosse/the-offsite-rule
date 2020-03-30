(ns the-offsite-rule.core.event
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core
             [location :as location]
             [journey :as journey]]))

(s/def ::participant (s/keys :req [::name string?
                                   ::location/location]
                             :opt [::journey/route]))

(s/def ::participants (s/coll-of ::participant))

(s/def ::event (s/keys :req [::participants
                             ::time inst?]))

(defn participant [name postcode postcode-converter]
  {::name name
   ::location/location (location/from-postcode postcode)})

(defn event [time participants]
  {:pre [(s/valid? inst? time)
         (s/valid? ::participants participants)]
   :post [(s/valid? ::event %)]}
  {::time time
   ::participants participants})
