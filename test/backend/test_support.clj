(ns backend.test-support
  (:require [clojure.edn :as edn]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [backend.app :refer [config]]
            ))

(defn test-db-uri
  []
  (str "datomic:mem://" (java.util.UUID/randomUUID)))

(defn test-config
  [db-uri]
  (assoc-in config [:db :uri] db-uri))

(defn read-json
  [path]
  (-> (str "test/" path ".json") slurp json/decode json/encode))

(defn read-edn
  [path]
  (edn/read-string {:readers *data-readers*}
                   (-> (str "test/" path ".edn") slurp)))

(defn is-response-json
  [response]
  (is (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8")))

(defn is-response-created
  [response expected-body config]
  (let [location (get-in response [:headers "Location"])]
    (is (= (:status response) 201))
    (is-response-json response)
    (is (= (get-in response [:headers "ETag"]) "1"))
    (is (= (:body response) expected-body))
    (is (.startsWith location (get-in config [:app :deploy-url])))
    ))

(defn is-response-ok
  [response expected-body version]
  (is (= (:status response) 200))
  (is-response-json response)
  (is (= (get-in response [:headers "ETag"]) (str version)))
  (is (= (:body response) expected-body))
  )

(defn not-found-test
  [handler request]
  (let [response (handler request)
        response-body (read-json "backend/not-found-response")
        ]
    (is (= (:status response) 404))
    (is-response-json response)
    (is (= (:body response) response-body))
    ))
