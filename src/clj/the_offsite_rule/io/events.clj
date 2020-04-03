(ns the-offsite-rule.io.events
  (:require [the-offsite-rule.api.user :refer [EventGetter]]
            [the-offsite-rule.io.db :as db]
            [the-offsite-rule.core
             [event :as event]]))

(defrecord Getter [user-id]
  EventGetter
  (fetch-all-event-ids [self] (:result (db/fetch-user-events user-id)))
  (fetch-event-participants-by-id [self event-id] (:result (db/fetch-event-inputs event-id)))
  (update-event-people [self event-id people] (:result (db/store-input-locations people event-id))))
