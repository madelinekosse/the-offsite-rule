(ns the-offsite-rule.routing
  (:require [reitit.frontend :as reitit]
            [the-offsite-rule.edit :as edit]
            [the-offsite-rule.results :as results]
            [the-offsite-rule.event-list :as event-list]))


(def router
  (reitit/router
   [["/" :index]
    ["/results"
     ["/:event-id" :results]]
    ["/edit"
     ["/:event-id" :event]]
    ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(defn home-page []
  (event-list/page))


(defn edit-page []
  (edit/page))


(defn results-page []
  (results/page))


(defn about-page []
  (fn [] [:span.main
          [:h1 "About the-offsite-rule"]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :about #'about-page
    :results #'results-page
    :event #'edit-page))
