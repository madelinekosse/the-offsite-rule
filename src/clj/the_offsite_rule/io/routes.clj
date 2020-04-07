(ns the-offsite-rule.io.routes
  (:require [the-offsite-rule.io.http :as http]
            [the-offsite-rule.core
             [event-location :refer [RouteFinder] :as event-location]
             [leg :as leg]
             [journey :as journey] ;;validate route spec
             [location :as location]]
            [clojure.spec.alpha :as s]
            [clj-time.coerce :as ct]))

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

(defn- station->location [station]
  (let [lat (get station "y")
        lng (get station "x")]
    (merge
     {::location/name (get station "name")}
     (location/from-coordinates lat lng))))

(defn- parse-waypoint [waypoint]
  (let [location-data (if (contains? waypoint "Addr")
                        (get waypoint "Addr")
                        (get waypoint "Stn"))
        time (get waypoint "time")]
    {:location (station->location location-data)
     :time (ct/from-string time)}))

(defn- connection->leg [connection]
  (let [transport-mode (get connection "mode")
        dep-loc (parse-waypoint (get connection "Dep"))
        arr-loc (parse-waypoint (get connection "Arr"))]
    #::leg{:start-time (:time dep-loc)
          :end-time (:time arr-loc)
          :start-location (:location dep-loc)
          :end-location (:location arr-loc)
          :transport-type (get transport-modes transport-mode)}))

(defn parse-connection-list [connections]
  (map connection->leg connections))

(defn- coordinate-string [coordinates]
  (str (::location/latitude coordinates)
       ", "
       (::location/longitude coordinates)))

(defn- request [from to arrival-time]
  (http/get
   url
   {:apiKey api-key
    :routing "all"
    :dep (coordinate-string (::location/coordinates from))
    :arr (coordinate-string (::location/coordinates to))
    :arrival 1
    :time (str arrival-time)}))

(defn- get-route [from to arrival-time]
  (-> (request from to arrival-time)
      (get "Res")
      (get "Connections")
      (get "Connection")
      first
      (get "Sections")
      (get "Sec")
      parse-connection-list))

(defrecord Finder []
    RouteFinder
  (route [self from to arrival-time]
    {:pre [(s/valid? ::location/location from)
           (s/valid? ::location/location to)
           (s/valid? inst? arrival-time)]
     ;:post [(s/valid? ::journey/route %)]
     }
    (get-route from to arrival-time)))
