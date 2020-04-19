(ns the-offsite-rule.event-list
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]))

(defonce events (atom []))

;;TODO: implement different users!
(defn- load-events []
  (go (let [response (<! (http/get "/api/events"
                                   {:query-params {:user-id 1}}))]
        (reset! events (:body response)))))

(defn event-list [path-finder-func]
  (fn[]
    (let [event-list @events]
      (if (empty? event-list)
        [:div "Loading..."]
        [:ul (map (fn [event]
                  [:li {:name "blah" :key (str "event-" {:id event})}
                   [:a {:href (path-finder-func :event {:event-id (:id event)})}
                    (str (:name event) "---" (:time event))]])
                event-list)]))))

(defn page [path-finder-func]
  (do (load-events)
      [:span.main
       [:h1 "All events"]
       [event-list path-finder-func]]))
