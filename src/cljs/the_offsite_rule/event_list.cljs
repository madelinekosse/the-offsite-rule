(ns the-offsite-rule.event-list
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [the-offsite-rule.components.table :as table]))

(defonce events (atom []))
(defonce error (atom nil))

;;TODO: implement different users!
(def user-id 1)

(defn- load-events []
  (go (let [response (<! (http/get "/api/events"
                                   {:query-params {:user-id user-id}}))]
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

(defn- add-event [event]
  (let [{:keys [name time]} @event]
    (println (str "new event: " name " " time))
    (go (let [response (<! (http/post "/api/new-event"
                                      {:form-params {:name name
                                                     :time (prn-str time)
                                                     :user-id user-id}}))]
          (if (not= 200 (:status response))
            (reset! error (:body response))
            (do (reset! error nil)
                (swap! events #(concat [@event] %))))))))

(defn- remove-element [element list]
  (remove #(= % element) list))

(defn- remove-event [event]
  (go (let [response (<! (http/post "/api/delete-event"
                                    {:form-params {:event-id (prn-str (:id event))
                                                   :user-id user-id}}))]
        (if (not= 200 (:status response))
          (reset! error (:body response))
          (do (reset! error nil)
              (swap! events (partial remove-element event)))))))

(defn event-list [path-finder-func]
  (fn[]
    (let [event-list @events
          ]
      (if (empty? event-list)
        [:div "Loading..."]
        [table/editable-table
         (columns path-finder-func)
         events
         add-event
         remove-event]))))

(defn page [path-finder-func]
  (do (load-events)
      [:span.main
       [:h1 "All events"]
       [event-list path-finder-func]]))
