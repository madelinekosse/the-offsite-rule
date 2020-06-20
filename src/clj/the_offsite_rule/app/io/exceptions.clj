(ns the-offsite-rule.app.io.exceptions)

(defn network-exception [message]
  "return exception with code for service unavailable when network is down"
  (ex-info message
           {:response-code 503}))

(defn bad-gateway-exception [message]
  "exception when call to API fails"
  (ex-info message
           {:response-code 502}))

(defn internal-exception [message]
  (ex-info message
           {:response-code 500}))

(defn bad-input-exception [message]
  (ex-info message
           {:response-code 400}))

(defn exception-reponse [exception]
  {:status (-> exception
               ex-data
               (get :response-code 500))
   :headers {}
   :body (ex-message exception)})

(defn client-error-response [message]
  "Helper for when theres no exception and you just want to return an error message"
  {:status 400
   :headers {}
   :body {:message message}})
