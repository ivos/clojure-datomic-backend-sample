(ns backend.datomic
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            ))

(defn- prepare-query-value
  [db value]
  (cond
    (nil? value) :nil
    (and (string? value) (empty? value)) :nil
    (keyword? value) (d/entid db value)
    :otherwise value
    ))

(defn prepare-query-params
  [data attributes db]
  (let [prepare-query-param #(vector % (prepare-query-value db (get data %)))]
    (into {} (map prepare-query-param attributes))))

(defn query-string
  [db-value query-param]
  (or (= :nil query-param)
      (.contains (.toLowerCase db-value) (.toLowerCase query-param))))

(defn query-keyword
  [db-value query-param]
  (or (= :nil query-param)
      (= db-value query-param)))
