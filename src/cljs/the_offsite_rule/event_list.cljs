(ns the-offsite-rule.event-list
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<!]]
   [the-offsite-rule.routing :as routing]
   [the-offsite-rule.components.table :as table]))

(defonce events (atom nil))
(defonce error (atom nil))

;;TODO: implement different users!
(def user-id 1)

(defn- load-events []
  (go (let [response (<! (http/get "/api/events"
                                   {:query-params {:user-id user-id}}))]
        (reset! events (:body response)))))

(defn- columns []
  {:name {:header "Name"
          :display-func (fn[event]
                          [:a {:href (routing/path-for :event {:event-id (:id event)})}
                           (:name event)])
          :input-type :text}
   :time {:header "Time"
          :display-func #(str (:time %))
          :input-type :datetime-local}})

(defn- add-event [event]
  (let [{:keys [name time]} @event]
    (go (let [response (<! (http/post "/api/new-event"
                                      {:json-params {:name name
                                                     :time (str time ":00.000Z")
                                                     :user-id user-id}}))]
          (if (not= 200 (:status response))
            (reset! error (:body response))
            (do (reset! error nil)
                (swap! events #(concat [(:body response)] %))))))))

(defn- remove-element [element list]
  (remove #(= % element) list))

(defn- remove-event [event]
  (go (let [response (<! (http/post "/api/delete-event"
                                    {:json-params {:event-id (:id event)
                                                   :user-id user-id}}))]
        (if (not= 200 (:status response))
          (reset! error (:body response))
          (do (reset! error nil)
              (swap! events (partial remove-element event)))))))

(defn event-list []
  (fn[]
    (let [event-list @events
          ]
      (if (nil? event-list)
        [:div "Loading..."]
        [table/editable-table
         (columns)
         events
         add-event
         remove-event]))))

(defn page []
  (do (load-events)
      [:span.main
       [:h1 "All events"]
       [event-list]]))
