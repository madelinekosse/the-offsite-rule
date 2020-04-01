(ns the-offsite-rule.core.leg
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core
             [location :as l]]))

(s/def ::start-location #(s/valid? ::l/location %))
(s/def ::end-location #(s/valid? ::l/location %))
(s/def ::transport-type #{"train", "bus", "walk", "wait", "taxi"})
(s/def ::start-time inst?)
(s/def ::end-time inst?)

(defn- valid-times? [leg]
  (t/before? (::start-time leg)
             (::end-time leg)))

(s/def ::leg (s/and (s/keys :req [::start-location
                                  ::start-time
                                  ::end-location
                                  ::end-time
                                  ::transport-type])
                    valid-times?))
