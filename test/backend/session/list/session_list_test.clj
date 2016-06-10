(ns backend.session.list.session-list-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [clj-time.core :as t]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  []
  (mock/request :get "/sessions/active"))

(deftest session-list-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/session/list/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (facts
      "Default"
      (let [request (create-request)
            response (t/do-at std-time (handler request))
            response-body (read-json "backend/session/list/response")
            ]
        (is-response-ok response response-body)
        ))
    ))
