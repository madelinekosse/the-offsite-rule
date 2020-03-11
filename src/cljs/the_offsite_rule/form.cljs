(ns the-offsite-rule.form
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]))

(defn person-row [people]
    [:div
     [:input {:type :text
              :placeholder "Name"
              :on-change (fn [e] (swap! person #(assoc % :name (-> e .-target .-value))))}]
     [:input {:type :text
              :placeholder "Postcode"
              :on-change (fn [e] (swap! person #(assoc % :postcode (-> e .-target .-value))))}]
     ])

(defn add-person [people]
  (swap! people #(conj % {})))

(defn submit-people [people]
  (let [data (->> people
                  (remove #(= % {}))
                  (map (fn [x] @x)))]
    (http/post "/api/save" {:form-params {:people (prn-str data)}})))

(defn table-row [record]
  [:tr
   [:td (:name record)]
   [:td (:postcode record)]])

(defn table [people]
  [:table
   [:tr
    [:th "Name"]
    [:th "Postcode"]]
   (for [row @people]
     (table-row row))])

(defn entry-rows [num-people]
  (let [people (atom [{:name "m" :postcode "m"}])]
    [:div
     (table people)
     [:input {:type :button
              :value :submit
              :on-click #(submit-people people)}]
     ]))
(defn postcode-entry-form []
  [:div "form goes here"
   (entry-rows 2)])
