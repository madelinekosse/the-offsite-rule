(ns the-offsite-rule.handler
  (:require
   [reitit.ring :as reitit-ring]
   [ring.middleware.defaults :refer :all]
   [ring.middleware.json :as json]
   [ring.util.response :refer [response]]
   [the-offsite-rule.middleware :refer [middleware]]
   [the-offsite-rule.db :as db]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]
   [clojure.walk :refer :all]
   [clojure.edn :as edn]))

(def mount-target
  [:div#app
   [:h2 "Welcome to the-offsite-rule"]
   [:p "please wait while Figwheel is waking up ..."]
   [:p "(Check the js console for hints if nothing exciting happens.)"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")]))


(defn index-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})


(defn api-handler
  [_request]
  (let [data (-> _request
                 :form-params
                 keywordize-keys
                 :people
                 edn/read-string)]
    (db/store-input-locations data)
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body "{\"a\": \"B\"}"}))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/results" {:get {:handler index-handler}}]
     ["/new" {:get {:handler index-handler}}]
     ["/about" {:get {:handler index-handler}}]
     ["/api" ["/save" {:post {:handler api-handler}}]]])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))


