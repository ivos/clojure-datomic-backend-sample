(ns backend.app
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [backend.config :refer :all]
            [backend.db :refer :all]
            [backend.router :refer :all]))

(def ^:private config (read-config))

(defn -main
  [& args]
  (let [db-uri (get-in config [:db :uri])]
    (log/info "Starting Backend, config:" config)
    (start-database! db-uri)
    (start-router! config)
    ))

(def repl-handler (handler config))
