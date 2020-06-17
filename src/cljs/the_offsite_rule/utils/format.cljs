(ns the-offsite-rule.utils.format)

(defn duration-map->string [{:keys [hours minutes seconds]}]
  (str hours "h " minutes "m " seconds "s"))
