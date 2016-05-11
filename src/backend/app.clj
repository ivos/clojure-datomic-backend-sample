(ns backend.app
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [backend.config :refer :all]
            [backend.db :refer :all]
            [backend.router :refer :all]))

(defn -main
  [& args]
  (log/infof "Starting Backend, DB config %s, router config %s." db-config router-config)
  (start-database! (:uri db-config))
  (start-router! router-config)
  )
