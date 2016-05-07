(ns backend.app
  (:gen-class)
  (:require [backend.db :as db]))

(def uri "datomic:free://localhost:4334/backend")
;(def uri "datomic:mem://backend")

(defn -main
  [& args]

;  (db/delete-database! uri)  
  (db/start! uri)  
  (println "Hello, World 3!")
  )
