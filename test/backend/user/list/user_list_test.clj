(ns backend.user.list.user-list-test
  (:require [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [params]
  (mock/request :get "/users" params))

(facts
  "User list"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/user/list/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (facts
      "Full query"
      (let [params {:username "uSeRnAmE-kEy"
                    :email "eMaIl-KeY"
                    :fullName "nAmE-kEy"}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/user/list/full-query-response")
            ]
        (is-response-ok response response-body)
        ))
    (facts
      "Empty query"
      (let [params {:username ""
                    :email ""
                    :fullName ""}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/user/list/no-query-response")
            ]
        (is-response-ok response response-body)
        ))
    (facts
      "No query"
      (let [params {}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/user/list/no-query-response")
            ]
        (is-response-ok response response-body)
        ))
    ))
