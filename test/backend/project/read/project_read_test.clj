(ns backend.project.read.project-read-test
  (:require [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [id]
  (mock/request :get (str "/projects/" id)))

(facts
  "Project read"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/read/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (fact
      "Full"
      (let [request (create-request "code-1")
            response (handler request)
            response-body (read-json "backend/project/read/full-response")
            ]
        (is-response-ok-version response response-body 123)
        ))
    (fact
      "Not found"
      (not-found-test handler (create-request 10)))
    (fact
      "Invalid id"
      (not-found-test handler (create-request "invalid")))
    ))
