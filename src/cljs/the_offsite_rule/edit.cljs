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


(defn submit-people [people event-id]
  (go (let [response (<! (http/post "/api/save"
                                    {:form-params {:people (prn-str @people)
                                                   :event-id event-id}}))
            _ (print response)]
        (if (not= 200 (:status response))
          (reset! error (:body response))
          (reset! error nil)))))

(defn update-event [event-id]
  (go (let [response (<! (http/get "/api/event"
                                   {:query-params {:event-id event-id}}))]
        (reset! people (get-in response [:body :event-participants]))
        (reset! event (select-keys (:body response) [:name :time :id])))))

(defn error-display []
  (fn[]
    (let [e @error]
      (if (some? e)
        [:div e]))))

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

(defn content []
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
           people]
          [:input {:type :button
                   :value :submit
                   :on-click #(submit-people people event-id)}]
          [error-display]]]))))

(defn page []
  (fn []
    (reset! people [])
    (reset! event {})
    (reset! error nil)
    [content]))
