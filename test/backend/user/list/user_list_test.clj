(ns backend.user.list.user-list-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [params]
  (mock/request :get "/users" params))

(deftest user-list-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/user/list/full-setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (testing
      "Full query"
      (let [params {:username "uSeRnAmE-kEy"
                    :email "eMaIl-KeY"
                    :full-name "nAmE-kEy"}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/user/list/full-query-response")
            ]
        (is-response-ok response response-body)
        ))
    (testing
      "Empty query"
      (let [params {:username ""
                    :email ""
                    :full-name ""}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/user/list/no-query-response")
            ]
        (is-response-ok response response-body)
        ))
    (testing
      "No query"
      (let [params {}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/user/list/no-query-response")
            ]
        (is-response-ok response response-body)
        ))
    ))
