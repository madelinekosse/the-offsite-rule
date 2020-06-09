(ns the-offsite-rule.app.io.http
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defn post [url body]
  (-> (client/post url
                   {:form-params body
                    :content-type :json})
      :body
      json/decode))

(defn get [url route-params]
  (-> (client/get url {:query-params route-params})
      :body
      json/decode))
