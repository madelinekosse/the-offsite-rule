(ns the-offsite-rule.handler
  (:require
   [reitit.ring :as reitit-ring]
   [ring.middleware.defaults :refer :all]
   [ring.middleware.json :as json]
   [ring.util.response :refer [response]]
   [the-offsite-rule.middleware :refer [middleware]]
   [the-offsite-rule.api.api :as api]
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

(defn get-event-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"a\": \"B\"}"})

(defn- extract-params [_request] ;;TODO eventually we want this to return body also
  (->> _request
       :params
       (reduce-kv
        (fn[m k v]
          (assoc m (keyword k) (edn/read-string v)))
        {})))

(defn- error-response [message]
  {:status 400
   :headers {}
   :body message})

(defn- success-response [result]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body result})

(defn api-handler [operation request]
  (let [params (extract-params request)
        result (case operation
                 :save-event-data (api/save-event-participants params)
                 :get-event-data  (api/get-event-participants params)
                 {:error (str "No handler registered for " operation)})]
    (if (:error result)
      (error-response (:error result))
      (success-response result))))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/results" {:get {:handler index-handler}}]
     ["/edit"
      ["/:event-id" {:get {:handler index-handler
                           :parameters {:path {:event-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]
     ["/api"
      ["/save" {:post {:handler (partial api-handler :save-event-data)}}]
      ["/event" {:get {:handler (partial api-handler :get-event-data)
                       :parameters {:query-params {:event-id int?}}}}]]])

   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))


