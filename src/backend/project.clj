(ns backend.project
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            [backend.entity :refer :all]
            ))

(def ^:private db-partition :db.part/backend)
(def ^:private attributes [:project/name :project/code :project/visibility])

(defn project-create
  [request]
  (let [conn (:connection request)
        tempid (d/tempid db-partition -1)
        data (-> (:body request)
               (ns-keys attributes)
               (ns-value :project/visibility :project.visibility)
               (assoc :id tempid))
        _ (log/info "Create project" data)
        tx (entity-create-tx db-partition attributes data)
        _ (log/trace "Tx" tx)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        id (d/resolve-tempid (d/db conn) (:tempids tx-result) tempid)
        saved (merge {:db/id id} (d/touch (d/entity (d/db conn) id)))
        _ (log/debug "Saved" saved)
        result (-> saved
                 (strip-value-ns :project/visibility)
                 strip-keys-ns)
        _ (log/info "Result" result)
        ]
    {:status 200
     :body result}))
