(ns the-offsite-rule.form
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-http.client :as http]))

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
  (let [rows @people]
    [:table
     [:tr
      [:th "Name"]
      [:th "Postcode"]]
     (for [row (reverse rows)]
       (table-row row))
     (submission-row)]))

(defn submit-people [people]
  (http/post "/api/save" {:form-params {:people (prn-str @people)}}))

(defn postcode-entry-form []
    [:div
     (table)
     [:input {:type :button
              :value :submit
              :on-click #(submit-people people)}]])
