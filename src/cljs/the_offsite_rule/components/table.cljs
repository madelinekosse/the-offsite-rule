(ns the-offsite-rule.components.table)

;;TODO make these all reusable for people or events page
(defn remove-element [element list]
  (remove #(= % element) list))

(defn add-element [element list]
  (let [element @element]
    (concat [element] list)))

(defn- format-element [record key header-meta]
  (let [display-func (:display-func header-meta)]
    [:td (display-func record)]))

(defn table-row [record header-lookup rows-to-edit]
  (into [:tr] (conj (map (fn[k] (format-element record k (get header-lookup k)))
                         (keys header-lookup))
                    [:td [:input {:type
                                  :button
                                  :value "-"
                                  :on-click #(swap! rows-to-edit
                                                    (partial remove-element record))}]])))

(defn- key->id [key]
  (-> key
      name
      (str "-input")
      keyword))

(defn- input-box [header-lookup record key]
  [:td [:input {:type (get-in header-lookup [key :input-type])
                :id (key->id key)
                :on-change (fn [e] (swap! record #(assoc % key (-> e .-target .-value))))}]])

(defn- submit-row-box [input-field-names rows-to-edit new-row]
  [:input
   {:type :button
    :value "+"
    :on-click (fn [e]
                (for [field input-field-names]
                  (set! (.-value (js/document.getElementById field)) ""));; TODO: why this no work?
                (swap! rows-to-edit #(add-element new-row %)))}])

(defn submission-row [header-lookup rows-to-edit]
  (let [new-row (atom {})
        input-names (->> header-lookup
                         keys
                         (map key->id)
                         (map name)
                         (map str))]
    (into [:tr]
          (conj (map (partial input-box header-lookup new-row)
                     (keys header-lookup))
                [:td (submit-row-box input-names rows-to-edit new-row)]))))

;; header lookup is an ordered map {:key-in-row "header name"}
;; rows-to-edit is an atom
;; it will be updated whenever stuff changes
;; TODO: add a way to include column types (for input)
;; it should allow text input and links (with the link function passed in)
;; header lookup : {:name {:header "Name" :display-func (str (:name %)) :input-type :text}} is ordered map
(defn editable-table [header-lookup rows-to-edit]
  (fn[]
    (let [rows @rows-to-edit]
      (println rows)
      [:table
       (into [:tr]
             (conj (map (fn [h] [:th (:header h)]) (vals header-lookup))
                   [:td]))
       (for [row (reverse rows)]
         (table-row row header-lookup rows-to-edit))
                                        (submission-row header-lookup rows-to-edit)])))
