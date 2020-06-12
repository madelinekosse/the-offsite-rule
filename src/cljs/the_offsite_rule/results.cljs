(ns the-offsite-rule.results
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [cljs-http.client :as http]
            [reagent.session :as session]
            [cljs-time.coerce :as ct]
            [cljs-time.core :as t]
            [cljs.core.async :refer [<!]]))

;;TODO: implement users
(def user-id 1)

(defonce locations (atom []))
(defonce error (atom nil))
(defonce loading (atom true))

(defn- handle-success [{:keys [last-update last-simulation] :as res}]
  (let [last-update-time (ct/from-string last-update)
        last-sim-time (if (= last-simulation "")
                        nil
                        (ct/from-string last-simulation))]
    (if (nil? last-sim-time)
      (reset! error "Simulation running... refresh in a minute")
      (if (t/before? last-update-time last-sim-time)
        (reset! error (str "Output is outdated; last run at " last-simulation))
        (reset! locations (:locations res))))))

(defn- load-locations [event-id]
  (reset! loading true)
  (go (let [response (<! (http/get "/api/locations"
                                   {:query-params {:event-id event-id
                                                   :user-id user-id}}))]
        (if (= 200 (:status response))
          (do
            (reset! loading false)
            (reset! locations (:body response)))
          (do
            (reset! loading false)
            (reset! error (:body response)))))))

(defn- record->row [location]
  [:tr [:td (:name location)]
   [:td (:total-time location)]
   [:td "link to route info"]])

(defn location-table [locations]
  [:table
   [:tr [:th "Location"]
    [:th "Total time"]
    [:th "More info"]]
   (into [:tbody] (mapv record->row locations))])

(defn table []
  (fn []
    (let [locations @locations
          error @error
          loading @loading
          _ (println locations)]
      (if loading
        [:div "Loading..."]
        (if (some? error)
          [:div (str "An error has occurred: " error)]
          [location-table locations])))))

(defn page []
  (fn []
    (let [routing-data (session/get :route)
          event-id (get-in routing-data [:route-params :event-id])]
      (do (load-locations event-id)
          [:span.main
           [:h1 (str "Results for event " event-id)]
           [table]]))))
