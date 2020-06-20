(ns the-offsite-rule.app.io.map
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clj-time.core :as t]
            [clj-time.coerce :as ct]
            [the-offsite-rule
             [search :as s]
             [location :as l]
             [journey :as j]]
            [the-offsite-rule.app.io.http :as http]
            [the-offsite-rule.app.io.exceptions :as ex]))

(def cities
  (let [rows (with-open [in-file (io/reader (.getFile (io/resource "cities.csv")))]
               (doall
                (csv/read-csv in-file)))
        headers (first rows)
        data (rest rows)]
    (->> data
         (map (fn[r] (zipmap headers r)))
         (map (fn[{:strs [lat lng city]}]
                (let [coords (map edn/read-string [lat lng])]
                  (l/location coords {:name city})))))))

;;TODO: extract
(def api-key "CJ4tYNXsbDPI1f6O-ZMo51rk1JkXR2S9I06UHe_buOM")

(def url "https://transit.ls.hereapi.com/v3/route.json")

(def transport-modes {0 "high speed train"
                      1 "train"
                      2 "train"
                      3 "train"
                      4 "train"
                      5 "bus"
                      6 "ferry"
                      7 "underground"
                      8 "light rail"
                      9 "taxi/drive"
                      10 "furnicular"
                      11 "cable car"
                      12 "coach"
                      13 "monorail"
                      14 "plane"
                      20 "walk"})

(defn- coordinate-string [coordinates]
  "Convert a coordinate pair to string for request"
  (str (::l/latitude coordinates)
       ", "
       (::l/longitude coordinates)))

(defn- request [from to arrival-time]
  "Make a request for directions from the API"
  (try
    (http/get
     url
     {:apiKey api-key
      :routing "all"
      :dep (coordinate-string (::l/coordinates from))
      :arr (coordinate-string (::l/coordinates to))
      :arrival 1
      :time (str arrival-time)})
    (catch java.io.IOException e
      (throw (ex/network-exception "Failed to connect to transport API"))
      )))


(defn- get-route [from to arrival-time]
  "Make API request and return a list of journey legs or thro exception"
  (let [response (request from to arrival-time)
        routes (get-in response ["Res" "Connections" "Connection"])]
    (if routes
      (-> routes
          first
          (get-in ["Sections" "Sec"]))
      (-> response
          (get-in ["Res" "Message" "text"])
          ((fn[m] (str "Route API failure: " m)))
          (ex/bad-gateway-exception)
          throw))))

(defn- parse-waypoint [waypoint]
  (let [location-data (if (contains? waypoint "Addr")
                        (get waypoint "Addr")
                        (get waypoint "Stn"))
        time (get waypoint "time")]
    {:location (l/location [(get location-data "y") (get location-data "x")]
                           {:name (get location-data "name")})
     :time (ct/from-string time)}))

(defn- parse-leg [connection]
  (let [transport-mode (get connection "mode")
        dep-loc (parse-waypoint (get connection "Dep"))
        arr-loc (parse-waypoint (get connection "Arr"))]
    #::j{:start-time (:time dep-loc)
         :end-time (:time arr-loc)
         :start-location (:location dep-loc)
         :end-location (:location arr-loc)
         :transport-type (get transport-modes transport-mode)}))

(defn- end-coords [route]
  "Find routes end location coordinates"
  (-> route
      last
      ::j/end-location
      ::l/coordinates))

(defn- start-coords [route]
  "Find routes start location coordinates"
  (-> route
      first
      ::j/start-location
      ::l/coordinates))

(defn- coordinates-equal? [c1 c2]
  "return true if these are the same coordinates"
  (let [coords-1 (->> c1
                      vals
                      (map float))
        coords-2 (->> c1
                      vals
                      (map float))]
    (= coords-1 coords-2)))

(defn add-start-end-locs [from to route]
  {:post [(coordinates-equal? (start-coords route) (start-coords %))
          (coordinates-equal? (end-coords route) (end-coords %))]}
  "Replace the routes start and end locations with those input so they include the name/postcode"
  (if (= 1 (count route))
    (-> route
        first
        (assoc ::j/start-location from)
        (assoc ::j/end-location to)
        vector)
    (let [new-first-leg (-> route
                            first
                            (assoc ::j/start-location from))
          new-last-leg (-> route
                           last
                           (assoc ::j/end-location to))]
      (-> route
          vec
          (assoc 0 new-first-leg)
          (assoc (dec (count route)) new-last-leg)))))

(defrecord MapApi []
  s/Map
  (-locations [self] cities)
  (-route [self from to arrival-time] (->> arrival-time
                                           (get-route from to)
                                           (map parse-leg)
                                           (add-start-end-locs from to))))
