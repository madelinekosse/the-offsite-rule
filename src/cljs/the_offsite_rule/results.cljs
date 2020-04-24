(ns the-offsite-rule.results
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [cljs-http.client :as http]
            [reagent.session :as session]
            [cljs.core.async :refer [<!]]))
;;TODO: implement users
(def user-id 1)

(defonce locations (atom []))
(defonce error (atom nil))

(defn- load-locations [event-id]
  (go (let [response (<! (http/get "/api/locations"
                                   {:query-params {:event-id event-id
                                                   :user-id user-id}}))]
        (if (= 200 (:status response))
          (reset! locations (:body response ))
          (reset! error (:body response))))))

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
          error @error]
      (if (empty? locations)
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
