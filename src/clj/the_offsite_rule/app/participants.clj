(ns the-offsite-rule.app.participants
  (:require [the-offsite-rule
             [participant :as p]
             [location :as l]
             [event :as e]]
            [clojure.spec.alpha :as s]
            [the-offsite-rule.value :as value]
            [the-offsite-rule.event :as e]))


;; Un-namespaced coordinates expected from the converter
(s/def ::coordinates (s/keys :req-un [::l/latitude
                                      ::l/longitude]))

;; input expected
(s/def ::name-postcode-pair (s/keys :req-un [::p/name
                                             ::l/postcode]))

(defn- valid-postcode-lookup? [lookup]
  (and (every? #(s/valid? ::l/postcode %) (keys lookup))
       (every? #(s/valid? ::coordinates %) (vals lookup))))

(defprotocol PostcodeConverter
  (-postcode-lookup [_ postcodes] "Return a lookup of postcode: coordinates"))

(defn- postcode-lookup [converter postcodes]
  {:pre [(satisfies? PostcodeConverter converter)
         (every? #(s/valid? ::l/postcode %) postcodes)]
   :post [(valid-postcode-lookup? %)]}
  (-postcode-lookup converter postcodes))

(defn new-participant [name postcode lat lng]
  {:post [(s/valid? ::p/participant %)]}
  "Create a new participant"
  (->> {:postcode postcode}
       (l/location [lat lng])
       (p/participant name)))


(defn participants [names-and-postcodes postcode-converter]
  {:pre [(every? #(s/valid? ::name-postcode-pair %) names-and-postcodes)]
   :post [(s/valid? ::e/participants %)]}
  "Create a whole list of participants and their locations using converter."
  (let [lookup (->> names-and-postcodes
                    (map :postcode)
                    (postcode-lookup postcode-converter))
        create-participant (fn[p] (new-participant (:name p)
                                                   (:postcode p)
                                                   (get-in lookup [(:postcode p) :latitude])
                                                   (get-in lookup [(:postcode p) :longitude])))]
    (map create-participant names-and-postcodes)))

(defn name-and-postcode [participant]
  {:pre [(s/valid? ::p/participant participant)]
   :post [(s/valid? ::name-postcode-pair %)]}
  (-> {}
      (assoc :name (::p/name participant))
      (assoc :postcode (get-in participant [::p/location ::l/postcode]))))

(defn location-for [participant]
  {:pre [(s/valid? ::p/participant participant)]
   :post [(s/valid? ::p/location %)]}
  (::p/location participant))

(defn name-for [participant]
  {:pre [(s/valid? ::p/participant participant)]
   :post [(s/valid? ::p/name %)]}
  (::p/name participant))
