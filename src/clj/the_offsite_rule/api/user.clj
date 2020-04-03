(ns the-offsite-rule.api.user)

;; TODO: implement seperate users

(defprotocol EventGetter
  (fetch-all-event-ids [self] "Return all event names and IDs for the user")
  (fetch-event-participants-by-id [self event-id] "Return the event participants with this ID")
  (update-event-people [self event-id people] "Save people as this event"))
