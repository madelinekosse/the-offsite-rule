(ns the-offsite-rule.core.journey
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [the-offsite-rule.core
             [leg :as leg]]))

(s/def ::duration-minutes (s/and int? pos?))

(defn- time-goes-forwards? [route]
  (let [times (->> route
                   (mapv (fn [l] [(::leg/start-time l) (::leg/end-time l)]))
                   flatten)]
    (= (sort times) times)))

(s/def ::route (s/and (s/coll-of ::leg/leg)
                      time-goes-forwards?))

(defn total-time-minutes [route]
  {:pre [(s/valid? ::route route)]
   :post [(s/valid? ::duration-minutes %)]}
  (let [start (::leg/start-time (first route))
        end (::leg/end-time (last route))]
    (t/in-minutes
     (t/interval start end))))
