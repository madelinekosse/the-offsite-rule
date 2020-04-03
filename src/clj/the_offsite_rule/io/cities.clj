(ns the-offsite-rule.io.cities
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [the-offsite-rule.core
             [search :refer [CityFinder] :as search]
             [location :as location]]))

(def filename (.getFile (io/resource "cities.csv")))

(defn- get-all []
  "returns a list of cities as maps"
  (let [rows (with-open [in-file (io/reader filename)]
               (doall
                (csv/read-csv in-file)))
        headers (first rows)]
    (mapv (fn [row] (zipmap headers row)) (drop 1 rows))))

(defn- map->location [city-row]
  (let [{:strs [lat lng city]} city-row]
    (-> (location/from-coordinates (edn/read-string lat) (edn/read-string lng))
        (assoc ::location/name city))))

(defrecord Finder []
  CityFinder
  (locations [self] (mapv map->location (get-all))))
