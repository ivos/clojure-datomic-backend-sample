(ns backend.config
  (:require [clojure.java.io :refer [resource]]
            [clojure.edn :as edn]))

(defn- parse-edn-resource
  [file-name]
  (-> file-name resource slurp edn/read-string))

(def db-config (parse-edn-resource "db-config.edn"))
(def router-config (parse-edn-resource "router-config.edn"))
(def json-config (parse-edn-resource "json-config.edn"))
