(ns backend.support.db
  (:require [io.rkn.conformity :as c]
            [datomic.api :as d]
            [clojure.tools.logging :as log]
            [backend.support.resources :refer [list-resources]]
            ))

(defn delete-database!
  [uri]
  (d/delete-database uri))

(defn- norm-key 
  "From a name in format \"some/dir/some-file-name.edn\" extracts keyword :some-file-name."
  [name]
  (-> name
    (.split "/")
    last
    (.split "\\.")
    first
    keyword))

(defn- wrapped-norm-content
  [name]
  {(norm-key name)
   {:txes [(c/read-resource name)]}})

(defn migrate!
  [uri]
  (let [conn (d/connect uri)
        names (sort (list-resources "db/migrations/"))
        norms-map (apply merge (map wrapped-norm-content names))
        _ (log/debug "Migrations loaded:" (keys norms-map))
        applied (c/ensure-conforms conn norms-map)
        ]
    (log/debug "Migrations applied:" (map :norm-name applied))
    ))

(defn start-database!
  "Connect to the database and migrate it."
  [uri]
  (d/create-database uri)
  (migrate! uri))
