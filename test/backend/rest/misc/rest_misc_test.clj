(ns backend.rest.misc.rest-misc-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.db :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(deftest method-not-allowed-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        ;setup (read-edn "backend/project/read/full-setup")
        ;db (:db-after @(d/transact (d/connect db-uri) setup))
        ]
    ; TODO uncomment
    '(testing
       "Invalid HTTP method"
       (let [response (handler (mock/request :patch (str "/projects/" 10)))
             ]
         (clojure.pprint/pprint response)
         (is (= (:status response) 405))
         (is (= (get-in response [:headers "Allow"]) "HEAD OPTIONS GET"))
         ))
    ))
