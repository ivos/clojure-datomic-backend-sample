(ns backend.datomic
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            ))

(defn prepare-query-params
  [data attributes]
  (let [prepare-query-value #(if (or (nil? %) (and (string? %) (empty? %))) :nil %)
        prepare-query-param #(vector % (prepare-query-value (get data %)))]
    (into {} (map prepare-query-param attributes))))

(defn query-string
  [db-value query-param]
  (or (= :nil query-param)
      (.contains (.toLowerCase db-value) (.toLowerCase query-param))))

(defn query-keyword
  [db db-value query-param]
  (or (= :nil query-param)
      (when-let [query-eids (d/q '[:find ?e :in $ ?param :where [?e :db/ident ?param]] db query-param)]
        (= db-value (ffirst query-eids)))))
