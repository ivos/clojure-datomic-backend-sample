(ns backend.test-support
  (:require [clojure.edn :as edn]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [midje.sweet :refer [fact]]
            [backend.app :refer [config]]
            [backend.support.ring :refer :all]
            ))

(def std-time (t/date-time 2015 10 11 12 34 56))

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
  (fact "Response content type"
        (get-in response [:headers "Content-Type"]) => "application/json; charset=utf-8"))

(defn is-response-created
  [response expected-body config]
  (let [location (get-in response [:headers "Location"])]
    (fact "Status code"
          (:status response) => (status-code :created))
    (is-response-json response)
    (fact "ETag"
          (get-in response [:headers "ETag"]) => "1")
    (fact "Response body"
          (:body response) => expected-body)
    (fact "Location start"
          (.startsWith location (get-in config [:app :deploy-url])) => true)
    ))

(defn is-response-ok
  [response expected-body]
  (fact "Status code"
        (:status response) => (status-code :ok))
  (is-response-json response)
  (fact "Response body"
        (:body response) => expected-body)
  )

(defn is-response-ok-version
  [response expected-body version]
  (fact "Status code"
        (:status response) => (status-code :ok))
  (is-response-json response)
  (fact "ETag"
        (get-in response [:headers "ETag"]) => (str version))
  (fact "Response body"
        (:body response) => expected-body)
  )

(defn is-response-conflict
  [response version]
  (fact "Status code"
        (:status response) => (status-code :conflict))
  (fact "ETag"
        (get-in response [:headers "ETag"]) => (str version))
  )

(defn is-response-precondition-required
  [response]
  (fact "Status code"
        (:status response) => (status-code :precondition-required))
  )

(defn not-found-test
  [handler request]
  (let [response (handler request)
        expected-body (read-json "backend/not-found-response")
        ]
    (fact "Status code"
          (:status response) => (status-code :not-found))
    (is-response-json response)
    (fact "Response body"
          (:body response) => expected-body)
    ))
