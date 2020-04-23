(ns the-offsite-rule.event-list
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [the-offsite-rule.components.table :as table]))

(defonce events (atom []))

;;TODO: implement different users!
(defn- load-events []
  (go (let [response (<! (http/get "/api/events"
                                   {:query-params {:user-id 1}}))]
        (reset! events (:body response)))))

(defn- columns [path-finder-func]
  {:name {:header "Name"
          :display-func (fn[event]
                          [:a {:href (path-finder-func :event {:event-id (:id event)})}
                           (:name event)])
          :input-type :text}
   :time {:header "Time"
          :display-func #(str (:time %))
          :input-type :text}})

(defn event-list [path-finder-func]
  (fn[]
    (let [event-list @events
          ]
      (if (empty? event-list)
        [:div "Loading..."]
        [table/editable-table (columns path-finder-func) events]))))

(defn page [path-finder-func]
  (do (load-events)
      [:span.main
       [:h1 "All events"]
       [event-list path-finder-func]]))
