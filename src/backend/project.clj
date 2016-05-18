(ns backend.project
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            [ring.util.response :refer :all]
            [bouncer.validators :as v]
            [backend.entity :refer :all]
            [backend.validation :refer [validate!]]
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
        _ (log/debug "Creating" data)
        _ (validate! data
                     :project/name v/required
                     :project/code v/required
                     :project/visibility v/required)
        tx (entity-create-tx db-partition attributes data)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        id (d/resolve-tempid db-after (:tempids tx-result) tempid)
        saved (merge {} (d/touch (d/entity db-after id)))
        _ (log/debug "Saved" saved)
        result (-> saved
                 (dissoc :entity/version)
                 (strip-value-ns :project/visibility)
                 strip-keys-ns)
        response (-> (str (get-in request [:config :app :deploy-url]) "projects/" id)
                   (created result)
                   (header "ETag" (:entity/version saved)))]
    response))
