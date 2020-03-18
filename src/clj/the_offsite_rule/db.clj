(ns the-offsite-rule.db
  (:require
   [clojure.java.jdbc :as j]
   [clojure.pprint :as p]))


(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "/home/mk-gsa/the-offsite-rule/the-offsite-rule"})

;;TODO: validate inputs (eg if theres no such event)
(defn store-input-locations [inputs event-id]
  (j/delete! db :person ["event_id = ?" event-id])
  (->> inputs
       (map #(assoc % :event_id event-id))
       (j/insert-multi! db :person))) ;; TODO: make this respond if it fails
