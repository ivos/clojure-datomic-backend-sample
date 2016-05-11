(ns backend.config
  (:require [clojure.java.io :refer [resource]]
            [clojure.edn :as edn]))

(def db-config (-> "db-config.edn" resource slurp edn/read-string))
(def router-config (-> "router-config.edn" resource slurp edn/read-string))
