(ns backend.project
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            [backend.entity :refer :all]
            ))

(def ^:private db-partition :backend)
(def ^:private attributes [:project/name :project/code :project/visibility])

(defn project-create
  [request]
  (let [conn (:connection request)
        data (-> (:body request)
               (ns-keys attributes)
               (ns-value :project/visibility :project.visibility))
        _ (log/debug "Create project, data" data)
        tx (entity-create-tx db-partition attributes data)
        _ (log/debug "Tx" tx)
        tx-result (d/transact conn tx)
        _ (log/debug "Tx result" tx-result)
        _ (log/debug "Resolved tempids" (:tempids tx-result))])
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "<h1>Created</h1><p>" request "</p>")})
