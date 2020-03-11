(ns the-offsite-rule.form
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]))

(defn person-row [person]
  [:div
   [:input {:type :text
            :placeholder "Name"
            :on-change (fn [e] (swap! person #(assoc % :name (-> e .-target .-value))))}]
   [:input {:type :text
            :placeholder "Postcode"
            :on-change (fn [e] (swap! person #(assoc % :postcode (-> e .-target .-value))))}]
   ])

(defn submit-people [people]
  (let [data (->> people
                  (remove #(= % {}))
                  (map (fn [x] @x)))]
    (http/post "/api/save" {:form-params {:people (prn-str data)}})))

(defn entry-rows [num-people]
  (let [people (repeat num-people (atom {}))]
    [:div
    (map person-row people)
     [:input {:type :button
              :value :submit
              :on-click #(submit-people people)}
      ]]))

(defn postcode-entry-form []
  [:div "form goes here"
   (entry-rows 2)])
