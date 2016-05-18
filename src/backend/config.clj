(ns backend.config
  (:require [clojure.java.io :refer [resource]]
            [clojure.edn :as edn]))

(defn- parse-edn-resource
  [file-name]
  (-> file-name resource slurp edn/read-string))

(defn read-config
  []
  (parse-edn-resource "config.edn"))
