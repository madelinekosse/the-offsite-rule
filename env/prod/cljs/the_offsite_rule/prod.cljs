(ns the-offsite-rule.prod
  (:require [the-offsite-rule.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
