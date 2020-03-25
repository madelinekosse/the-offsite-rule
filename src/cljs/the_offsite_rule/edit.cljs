(ns the-offsite-rule.edit
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]
            [reagent.session :as session]
            [cljs.core.async :refer [<!]]))

(defonce people (atom []))

(defn remove-person [person people]
  (remove #(= % person) people))

(defn table-row [record]
  [:tr
   [:td (:name record)]
   [:td (:postcode record)]
   [:td [:input {:type :button
                 :value "-"
                 :on-click #(swap! people (partial remove-person record))}]]])

(defn valid-person? [record]
  (let [{name :name postcode :postcode} record]
    (and (some? name)
         (some? postcode)
         (not (= name ""))
         (not (= postcode "")))))

(defn add-person [person people]
  (let [person @person]
    (if (valid-person? person)
      (conj people person)
      people)))

(defn submission-row []
  (let [person (atom {})]
    [:tr
     [:td
      [:input {:type :text
               :id :name-input
               :on-change (fn [e] (swap! person #(assoc % :name (-> e .-target .-value))))}]]
     [:td
      [:input {:type :text
               :id :postcode-input
               :on-change (fn [e] (swap! person #(assoc % :postcode (-> e .-target .-value))))}]]
     [:td
      [:input
       {:type :button
        :value "+"
        :on-click (fn [e]
                    (set! (.-value (js/document.getElementById "name-input")) "")
                    (set! (.-value (js/document.getElementById "postcode-input")) "")
                    (swap! people #(add-person person %)))}]]]))

(defn table []
  (fn[]
    (let [rows @people]
    [:table
     [:tr
      [:th "Name"]
      [:th "Postcode"]]
     (for [row (reverse rows)]
       (table-row row))
     (submission-row)])))

(defn submit-people [people event-id]
  (http/post "/api/save" {:form-params {:people (prn-str @people) :event-id event-id}}))

(defn update-people [event-id]
  (go (let [response (<! (http/get "/api/event"
                                   {:query-params {:event-id event-id}}))]
        (prn response)
        (reset! people (get-in response [:body :result])))))

(defn page []
  (fn []
      (let [routing-data (session/get :route)
            event-id (get-in routing-data [:route-params :event-id])]
        (do
          (update-people event-id)
          [:span.main
           [:h1 (str "Event " event-id)]
           [:div
            [table]
            [:input {:type :button
                     :value :submit
                     :on-click #(submit-people people event-id)}]]]))))
