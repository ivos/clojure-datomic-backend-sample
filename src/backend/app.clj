(ns backend.app
  (:gen-class)
  (:require [clojure.java.io :refer [resource]]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [backend.db :refer :all]
            [backend.router :refer :all]))

(def db-config (-> "db-config.edn" resource slurp edn/read-string))
(def router-config (-> "router-config.edn" resource slurp edn/read-string))

(defn -main
  [& args]

  (println "Resource" (resource "db-config.edn"))
  (println "Content" (slurp (resource "db-config.edn")))
  (println "URI" (:uri (slurp (resource "db-config.edn"))))
  (println "URI passed" (:uri db-config))

  (start-database! (:uri db-config))
  (start-router! router-config)
  )
