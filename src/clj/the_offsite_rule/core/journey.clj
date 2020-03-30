(ns the-offsite-rule.core.journey
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core [location :as location]]))

(s/def ::transport-type #{"train", "bus", "walk", "wait", "taxi"})

(s/def ::leg (s/keys :req [::start-location (s/valid? ::location)
                         ::end-location (s/valid? ::location)
                         ::transport-type
                         ::start-time inst?
                         ::end-time inst?]))

(s/def ::route (s/coll-of ::leg))

(defn total-time-minutes [route]
  {:pre [(s/valid? ::route route)]
   :post [(s/valid? int? %)]}
  (let [start (::start-time (first route))
        end (::end-time (last route))]
    (t/in-minutes
     (t/interval start end))))
