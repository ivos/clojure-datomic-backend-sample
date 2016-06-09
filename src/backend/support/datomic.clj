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

(defn maybe-eid
  [db type attribute value]
  (let [query [':find '?e
               ':in '$ '?type '?v
               ':where '[?e :entity/type ?type] ['?e attribute '?v]
               ]]
    (-> (d/q query db type value)
      ffirst)))

(defn verify-eid
  [eid]
  (if (nil? eid)
    (throw+ {:type :custom-response
             :response {:status (status-code :not-found)
                        :body {:code :entity.not.found}}})
    eid))

(defn get-eid
  [db type attribute value]
  (let [eid (maybe-eid db type attribute value)]
    (verify-eid eid)))

(defn get-entity
  [db eid]
  (merge {:eid eid} (d/touch (d/entity db eid))))

(defn expand-entity
  [data db key]
  (let [eid (get-in data [key :db/id])
        entity (get-entity db eid)]
    (assoc data key entity)))
