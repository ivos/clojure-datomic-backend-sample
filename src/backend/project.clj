(ns backend.project
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            ))

(def uri "datomic:free://localhost:4334/backend")

(defn project-create
  [request]
  (let [conn (d/connect uri)
        tx [
            [:db/add (d/tempid :db.part/user) :project/name "Bla"]
            ]
        tx-result (d/transact conn tx)]
    (log/debug "Create project 2:" request)
    (clojure.pprint/pprint request)
    (log/debug "Resolved tempids" (:tempids tx-result)))
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (str "<h1>Created</h1><p>" request "</p>")})
