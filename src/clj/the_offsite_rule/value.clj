(ns the-offsite-rule.value
  (:require [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.format :as ft]))

;;TODO: missing tests for these time ooperations
;; A namespace to store specs for common value types

(s/def ::name string?)

(s/def ::time inst?) ;;TODO investigate different time types and what this means

(s/def ::time-str  #(re-matches #"(\d{4})(-(\d{2}))(-(\d{2}))(T(\d{2}):(\d{2})(:(\d{2}))(\.(\d+))Z)" %))

(defn time->str [time]
  {:pre [(s/valid? ::time time)]
   :post [(s/valid? ::time-str %)]}
  "Convert time to timestamp format for front end such as 20200609T123212.798Z"
  (ft/unparse (ft/formatters :date-time) time))

(defn str->time [timestamp]
  {:pre [(s/valid? ::time-str timestamp)]
   :post [(s/valid? ::time %)]}
  (ft/parse (ft/formatters :date-time) timestamp))

(s/def ::id #(and (int? %) (>= % 0)))

(s/def ::duration #(instance? org.joda.time.ReadablePeriod %))


(s/def ::hours #(and (int? %) (>= % 0)))
(s/def ::minutes #(<= 0 % 60))
(s/def ::seconds #(<= 0 % 60))

(s/def ::duration-map  (s/keys :req-un [::hours
                                        ::minutes
                                        ::seconds]))

(defn duration->map [duration]
  {:pre [(s/valid? ::duration duration)]
   :post [(s/valid? ::duration-map %)]}
  "Convert a duration to a map of days, hours, minutes, seconds"
  (let [total-secs (t/in-seconds duration)
        secs (rem total-secs 60)
        total-mins (quot total-secs 60)
        mins (rem total-mins 60)
        hours (quot total-mins 60)]
    {:hours hours
     :minutes mins
     :seconds secs}))

(defn duration [start-time end-time]
  {:pre [(s/valid? ::time start-time)
         (s/valid? ::time end-time)]
   :post [(s/valid? ::duration %)]}
  "Return a new duratioin representing the start and end times"
  (-> start-time
      (t/interval end-time)
      t/in-seconds
      t/seconds))

(defn add-durations [durations]
  {:pre [(every? #(s/valid? ::duration %) durations)]
   :post [(s/valid? ::duration %)]}
  "For a list of durations, return a new duration that is the sum of them"
  (->> durations
       (map t/in-seconds)
       (apply +)
       t/seconds))

;;TODO: this is never used anywhere but it would be nice to include it alongside the total!
(defn average-duration [durations]
  {:pre [(every? #(s/valid? ::duration %) durations)]
   :post [(s/valid? ::duration %)]}
  "Return the average duration of all the journeys"
  (-> durations
      add-durations
      t/in-seconds
      (/ (count durations))
      (t/seconds)))
