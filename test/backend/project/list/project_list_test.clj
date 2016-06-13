(ns backend.project.list.project-list-test
  (:require [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [params]
  (mock/request :get "/projects" params))

(facts
  "Project list"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/list/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (fact
      "Full query"
      (let [params {:name "nAmE-kEy"
                    :code "CoDe-KeY"
                    :visibility "private"}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/project/list/full-query-response")
            ]
        (is-response-ok response response-body)
        ))
    (fact
      "Empty query"
      (let [params {:name ""
                    :code ""
                    :visibility ""}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/project/list/no-query-response")
            ]
        (is-response-ok response response-body)
        ))
    (fact
      "No query"
      (let [params {}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/project/list/no-query-response")
            ]
        (is-response-ok response response-body)
        ))
    ))
