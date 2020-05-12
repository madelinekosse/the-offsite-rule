(ns the-offsite-rule.edit
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [reagent.session :as session]
            [cljs.core.async :refer [<!]]
            [the-offsite-rule.components.table :as table]))

(defonce people (atom []))
(defonce event (atom {}))
(defonce error (atom nil))
(defonce saved? (atom true))

(defn submit-people [people event-id]
  (reset! saved? nil)
  (go (let [save-response (<! (http/post "/api/save"
                                    {:form-params {:people (prn-str @people)
                                                   :event-id event-id}}))
            trigger-response (<! (http/post "/api/trigger-run"
                                            {:form-params {:event-id event-id}}))]
        (if (not= 200 (:status save-response))
          (do (reset! error (:body save-response))
              (reset! saved? false))
          (do (reset! saved? true)
              (if (not= 200 (:status trigger-response))
                (reset! error "Failed to trigger run")))))))

(defn update-event [event-id]
  (go (let [response (<! (http/get "/api/event"
                                   {:query-params {:event-id event-id}}))]
        (reset! people (get-in response [:body :event-participants]))
        (reset! event (select-keys (:body response) [:name :time :id])))))

(defn error-display [link-to-results]
  (fn[]
    (let [e @error
          saved @saved?]
      (if (some? e)
        [:div e]
        (if (nil? saved)
          [:div "Saving..."]
          (if (true? saved)
            [:a {:href link-to-results} "Event Locations"]
            [:div "Unsaved changes"]))))))

(defn event-header []
  (fn[]
    (let [event-meta @event]
      [:div
       [:h1 (:name event-meta)]
       [:h2 (:time event-meta)]])))

(def columns (sorted-map :name {:header "Name"
                                :display-func #(str (:name %))
                                :input-type :text}
                         :postcode {:header "Postcode"
                                    :display-func #(str (:postcode %))
                                    :input-type :text}))

(defn add-person [new-person]
  (do (reset! saved? false)
      (swap! people #(concat [@new-person] %))))

(defn- remove-element [element list]
  (remove #(= % element) list))

(defn remove-person [person]
  (do (reset! saved? false)
      (swap! people (partial remove-element person))))

(defn content [path-finder-func]
  (fn[]
    (let [routing-data (session/get :route)
          event-id (get-in routing-data [:route-params :event-id])]
      (do
        (update-event event-id)
        [:span.main
         [event-header]
         [:div
          [table/editable-table
           columns
           people
           add-person
           remove-person]
          [:input {:type :button
                   :value :submit
                   :on-click #(submit-people people event-id)}]
          [error-display (path-finder-func
                          :results {:event-id event-id})]]]))))

(defn page [path-finder-func]
  (fn []
    (reset! people [])
    (reset! event {})
    (reset! error nil)
    (reset! saved? true)
    [content path-finder-func]))
