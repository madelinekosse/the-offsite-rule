(ns the-offsite-rule.db)

(defn store-input-locations [inputs]
  "For a list of maps containing user detail, write to file"
  (->> inputs
       (map #(str "Name: " (:name %) " Postcode: " (:postcode %) "\n"))
       (apply str)
       (spit "people.txt"))
  )
