(ns the-offsite-rule.middleware

  (:require
   [ring.middleware.content-type :refer [wrap-content-type]]
   [prone.middleware :refer [wrap-exceptions]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   ))

(def middleware
  [#(wrap-defaults % (assoc-in site-defaults [:security :anti-forgery] false))
   wrap-exceptions
   wrap-params
   wrap-keyword-params
   wrap-reload])
