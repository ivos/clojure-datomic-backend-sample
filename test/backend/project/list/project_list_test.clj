(ns backend.project.list.project-list-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.db :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [params]
  (mock/request :get "/projects" params))

(deftest project-list-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/list/full-setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (testing
      "Full query"
      (let [params {:name "nAmE-kEy"
                    :code "CoDe-KeY"
                    :visibility "private"}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/project/list/full-query-response")
            ]
        ;(clojure.pprint/pprint response)
        (is-response-ok response response-body)
        ))
    (testing
      "Empty query"
      (let [params {:name ""
                    :code ""
                    :visibility ""}
            request (create-request params)
            response (handler request)
            response-body (read-json "backend/project/list/no-query-response")
            ]
        ;(clojure.pprint/pprint response)
        (is-response-ok response response-body)
        ))
    ))
