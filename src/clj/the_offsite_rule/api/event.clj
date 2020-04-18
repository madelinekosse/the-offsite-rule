(ns the-offsite-rule.api.event
  (:require [the-offsite-rule.io
             [cities :as cities]
             [routes :as routes]
             [postcodes :as postcodes]]
            [the-offsite-rule.core
             [event :as event]
             [participant :as participant]]
            [the-offsite-rule.api
             [user :as user]]))

(defn- extract-postcodes [event-inputs]
  (->> event-inputs
       :event-participants
       (map :postcode)))

(defn- person->participant [converter {:keys [name postcode]}]
  (participant/participant name postcode converter))

(defn- event [event-inputs postcode-converter]
  (->> event-inputs
       :event-participants
       (map (partial person->participant postcode-converter))
       (event/event (:id event-inputs)
                    (:name event-inputs)
                    (:time event-inputs))))

(defn state [event-repository event-id]
  (let [event-inputs (user/event event-repository event-id)
        postcode-converter (-> event-inputs
                               extract-postcodes
                               postcodes/converter)]
    {:location-converter postcode-converter
     :city-finder (cities/->Finder)
     :route-finder (routes/->Finder)
     :event (event event-inputs postcode-converter)}))
