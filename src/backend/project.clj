(ns backend.project
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            ))

(defn project-create
  [request]
  (let [conn (:connection request)
        data (:body request)
        tx [
            [:db/add (d/tempid :db.part/user) :project/name "Bla"]
            ]
        tx-result (d/transact conn tx)]
    (log/debug "Create project:" data)
    (log/debug "Project name:" (:name data))
    (clojure.pprint/pprint request)
    (log/debug "Resolved tempids" (:tempids tx-result)))
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "<h1>Created</h1><p>" request "</p>")})
