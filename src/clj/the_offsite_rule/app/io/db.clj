(ns the-offsite-rule.app.io.db
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clj-time.core :as t]
   [clojure.string :as string]
   [the-offsite-rule.app.user :as user]
   [the-offsite-rule.app.event :as event]
   [the-offsite-rule.event :as e]
   [clj-time.coerce :as ct]
   [the-offsite-rule.app.io.exceptions :as ex]))


(defn file [user-id]
  (-> user-id
      (str "-user-data.edn")
      io/file))

(defn seconds-reader [object-string]
  (->> object-string
       last
       str
       (re-find  #"\d+")
       Integer/parseInt
       t/seconds))

(defn parse-str [file-contents]
  "Helper to read EDN times correctly"
  (->> file-contents
       (edn/read-string {:readers (merge ct/data-readers {'object seconds-reader})})))

(defn create-string [all-event-data]
  "Helper to write out durations as seconds"
  (-> all-event-data
      prn-str))

(defn load-file [user-id]
  "Load file contents if present, otherwise return empty list"
  (let [f (file user-id)
        contents (if (.exists f)
                   (-> f
                       slurp
                       parse-str)
                   [])]
    (if (coll? contents)
      contents
      (throw (ex/internal-exception "Failed to parse file")))))

(defn save-data [user-id data]
  "Writes data to user file, deleting previous"
  (let [f (file user-id)
        writer (io/writer f)]
    (.write writer (create-string data))
    (.close writer)))

(defn- event->summary [user-id e]
  (try (merge (select-keys e
                      [::event/id
                       ::event/last-simulation
                       ::event/last-update])
         (select-keys (::e/event e) [::e/name ::e/time])
         {::user/id user-id})
       (catch Exception e
         (throw (ex/internal-exception "File is corrupted")))))


(defn event-summaries [user-id]
  (->> user-id
       load-file
       (filter some?)
       (map (partial event->summary user-id))))

(defn next-event-id [user-id]
  (-> user-id
      load-file
      count))

(defn load-event [user-id event-id]
  (let [event (-> user-id
                  load-file
                  (nth event-id nil))]
    (if (some? event)
      event
      (throw (ex/bad-input-exception "Event does not exist")))))

(defn save-event [user-id event]
  (let [current (load-file user-id)
        new (if (or (empty? current) (= (::event/id event) (count current)))
              (conj current event)
              (assoc current (::event/id event) event))]
    (try (do (save-data user-id new)
             event)
         (catch Exception e
           (throw (ex/internal-exception "Failed to write to database"))))))

(defn delete-event [user-id event-id]
  (let [data (load-file user-id)]
    (if (>= event-id (count data))
      (throw (ex/bad-input-exception "Event does not exist"))
      (do (save-data user-id (assoc data event-id nil))
          true))))

(defrecord DB [user-id]
  user/EventRepository
  (-user-id [self] user-id)
  (-all-events [self] (event-summaries user-id))
  (-next-event-id [self] (next-event-id user-id))
  (-load-event [self event-id] (load-event user-id event-id))
  (-save-event [self event-state] (save-event user-id event-state))
  (-delete-event [self event-id] (delete-event user-id event-id)))
