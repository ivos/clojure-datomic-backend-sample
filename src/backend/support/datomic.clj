(ns backend.support.datomic
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            [slingshot.slingshot :refer [throw+]]
            [backend.support.ring :refer [status-code]]
            ))

(defn- prepare-query-value
  [db value]
  (cond
    (nil? value) :nil
    (and (string? value) (empty? value)) :nil
    (keyword? value) (if-let [eid (d/entid db value)]
                       eid
                       value)
    :otherwise value
    ))

(defn prepare-query-params
  [data attributes db]
  (let [prepare-query-param #(vector % (prepare-query-value db (get data %)))]
    (into data (map prepare-query-param attributes))))

(defn query-string
  [db-value query-param]
  (or (= :nil query-param)
      (.contains (.toLowerCase db-value) (.toLowerCase query-param))))

(defn query-keyword
  [db-value query-param]
  (or (= :nil query-param)
      (= db-value query-param)))

(defn get-eid
  [db id type]
  (let [query '[:find ?e
                :in $ ?e ?type
                :where [?e :entity/type ?type]]
        eid (-> (d/q query db id type)
              ffirst)]
    (if (nil? eid)
      (throw+ {:type :custom-response
               :response {:status (status-code :not-found)
                          :body {:code :entity.not.found}}})
      eid)))

(defn get-entity
  [db id]
  (merge {:id id} (d/touch (d/entity db id))))
