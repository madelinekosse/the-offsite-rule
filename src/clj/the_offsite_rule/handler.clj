(ns the-offsite-rule.handler
  (:require
   [reitit.ring :as reitit-ring]
   [ring.middleware.defaults :refer :all]
   [ring.util.response :refer [response]]
   [the-offsite-rule.middleware :refer [middleware]]
   [the-offsite-rule.app.io.format :as f]
   [the-offsite-rule.app.io.api :as api]
   [hiccup.page :refer [include-js include-css html5]]
   [config.core :refer [env]]
   [clojure.walk :refer :all]
   [clojure.edn :as edn]
   [clj-time.coerce :as ct]
   [the-offsite-rule.app.io.exceptions :as ex]))

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

(defn get-event-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"a\": \"B\"}"})

(defn- parse-params [_request] ;;TODO eventually we want this to return body also
  (->> _request
       :params
       (reduce-kv
        (fn[m k v]
          (if (= :time k)
            (assoc m (keyword k) v)
            (assoc m (keyword k) (edn/read-string v))))
        {})))

(defn- success-response [result]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body result})

(defn- call-api-func [function params]
  (try (-> params
           function
           success-response)
       (catch clojure.lang.ExceptionInfo e
         (ex/exception-reponse e))))

;; TODO: I dont think we need parse-params here at all... we don't get a time field at all so I'm not sure what its doing
(defn api-get-handler [operation request]
  (let [params (-> request
                   parse-params
                   (assoc :user-id 1))
        maybe-error (f/params-error? params)
        op-func (case operation
                  :get-event-data  api/get-event
                  :get-all-events api/list-events
                  :get-event-locations api/run-event
                  :get-location-details api/location-details
                  (fn[p] {:error (str "No get handler registered for " operation)}))]
    (if (some? maybe-error)
      (ex/client-error-response maybe-error)
      (call-api-func op-func params))))

;; TODO: hard coding the user ID as 1 for now
(defn api-post-handler [operation request]
  (let [params (-> request
                   :body
                   (assoc :user-id 1)
                   f/parse-request-body)
        maybe-error (f/params-error? params)
        op-func (case operation
                  :save-event-data api/edit-event
                  :new-event api/new-event
                  :delete-event api/delete-event
                  (fn[p] {:error (str "No post handler registered for " operation)}))]
    (if (some? maybe-error)
      (ex/client-error-response maybe-error)
      (call-api-func op-func params))))


;TODO: need a delete endpoint
;TODO: figure out the trigger vs run problem
;TODO: for all the posts, we'll want the user ID to be a query param and the content to be the body
(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/results"
      ["/:event-id" {:get {:handler index-handler
                           :parameters {:path {:event-id int?}}}}]]
     ["/edit"
      ["/:event-id" {:get {:handler index-handler
                           :parameters {:path {:event-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]
     ["/api"
      ["/save" {:post {:handler (partial api-post-handler :save-event-data)}}]
      ["/new-event" {:post {:handler (partial api-post-handler :new-event)}}]
      ["/delete-event" {:post {:handler (partial api-post-handler :delete-event)}}]
      ["/events" {:get {:handler (partial api-get-handler :get-all-events)
                        :parameters {:query-params {:user-id int?}}}}]
      ["/event" {:get {:handler (partial api-get-handler :get-event-data)
                       :parameters {:query-params {:event-id int?}}}}]
      ["/locations" {:get {:handler (partial api-get-handler :get-event-locations)
                           :parameters {:query-params {:event-id int?}}}}]
      ["/location-detail" {:get {:handler (partial api-get-handler :get-location-details)
                                 :parameters {:query-params {:event-id int?
                                                             :location-name string?}}}}]]])

   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))


