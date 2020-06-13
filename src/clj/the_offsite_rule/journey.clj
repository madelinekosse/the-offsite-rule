(ns the-offsite-rule.journey
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule
             [location :as l]
             [value :as value]]))

(s/def ::start-time #(s/valid? ::value/time %))
(s/def ::end-time #(s/valid? ::value/time %))
(s/def ::start-location #(s/valid? ::l/location %))
(s/def ::end-location #(s/valid? ::l/location %))
(s/def ::transport-type #{"train"
                          "bus"
                          "walk"
                          "high speed train"
                          "underground"
                          "ferry"
                          "light rail"
                          "furnicular"
                          "cable car"
                          "coach"
                          "monorail"
                          "plane"
                          "taxi/drive"})

(defn- valid-times? [leg]
  (t/before? (::start-time leg)
             (::end-time leg)))

(s/def ::leg (s/and (s/keys :req [::start-time
                                  ::end-time
                                  ::transport-type
                                  ::start-location
                                  ::end-location])
                    valid-times?))

;;TODO: journey only valid if the end location of each leg is the start of the first
(defn- time-goes-forwards? [legs]
  (let [times (->> legs
                   (mapv (fn [l] [(::start-time l) (::end-time l)]))
                   flatten)]
    (= (sort times) times)))

(s/def ::journey (s/and (s/coll-of ::leg)
                        time-goes-forwards?))

(defn total-time [journey]
  {:pre [(s/valid? ::journey journey)]
   :post [(s/valid? ::value/duration %)]}
  "Return duration of the entire journey"
  (let [start (::start-time (first journey))
        end (::end-time (last journey))]
    (value/duration start end)))

(defn num-changes [journey]
  {:pre [(s/valid? ::journey journey)]}
  (-> journey
      count
      dec))

(defn start-location [journey]
  {:pre [(s/valid? ::journey journey)]
   :post [(s/valid? ::l/location %)]}
  (-> journey
      first
      ::start-location))
