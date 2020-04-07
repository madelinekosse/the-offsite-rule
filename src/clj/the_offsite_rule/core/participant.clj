(ns the-offsite-rule.core.participant
  (:require [clojure.spec.alpha :as s]
            [the-offsite-rule.core
             [location :as location]
             [journey :as journey]]))

(s/def ::name string?)

(s/def ::participant (s/keys :req [::name
                                   ::location/location]
                             :opt [::journey/route]))

(defn participant [name postcode postcode-converter]
  {::name name
   ::location/location (location/from-postcode postcode postcode-converter)})
