(ns the-offsite-rule.location-detail
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.session :as session]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [reagent.core :as reagent :refer [atom]]
            [the-offsite-rule.utils.format :as format]))

(defonce routes (atom []))
(defonce error (atom nil))

(defn- load-routes [event-id location-name]
  (go (let [response (<! (http/get "/api/location-detail"
                                   {:query-params {:event-id event-id
                                                   :location-name (prn-str location-name)}
                                    }))]
        (if (= 200 (:status response))
          (reset! routes (:body response))
          (reset! error (:body response))))))

(defn- table []
  (fn[]
    (let [rows @routes]
    [:table
     [:tr
      [:th "Guest name"]
      [:th "Travel time"]
      [:th "Number of changes"]]
     (into [:tbody] (mapv (fn[r] [:tr
                                  [:td (:name r)]
                                  [:td (format/duration-map->string (:duration r))]
                                  [:td (:changes r)]])
                          rows))])))

(defn page []
  (fn []
    (let [routing-data (session/get :route)
          event-id (get-in routing-data [:route-params :event-id])
          location-name (get-in routing-data [:route-params :name])]
      (do
        (load-routes event-id location-name)
          [:span.main
           [:h1 (str "Results for event " event-id " and location " location-name)]
           [table]]))))
