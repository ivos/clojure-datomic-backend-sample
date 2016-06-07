(ns backend.user.read.user-read-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [id]
  (mock/request :get (str "/users/" id)))

(deftest user-read-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/user/read/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (testing
      "Full"
      (let [request (create-request "username-full")
            response (handler request)
            response-body (read-json "backend/user/read/full-response")
            ]
        (is-response-ok-version response response-body 123)
        ))
    (testing
      "Minimal"
      (let [request (create-request "username-minimal")
            response (handler request)
            response-body (read-json "backend/user/read/minimal-response")
            ]
        (is-response-ok-version response response-body 123)
        ))
    (testing
      "Not found"
      (not-found-test handler (create-request 10)))
    (testing
      "Invalid id"
      (not-found-test handler (create-request "invalid")))
    ))
