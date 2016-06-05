(ns backend.rest.misc.rest-misc-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.support.ring :refer :all]
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

(deftest invalid-enum-value-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)]
    (start-database! db-uri)
    (testing
      "Invalid enum value"
      (let [request-body (read-json "backend/rest/misc/invalid-enum-request")
            response-body (read-json "backend/rest/misc/invalid-enum-response")
            request (-> (mock/request :post "/projects" request-body)
                      (mock/content-type "application/json"))
            response (handler request)
            ]
        (is (= (:status response) (:unprocessable-entity status-code)))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    ))

(deftest invalid-attribute-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)]
    (start-database! db-uri)
    (testing
      "Invalid attribute"
      (let [request-body (read-json "backend/rest/misc/invalid-attribute-request")
            response-body (read-json "backend/rest/misc/invalid-attribute-response")
            request (-> (mock/request :post "/projects" request-body)
                      (mock/content-type "application/json"))
            response (handler request)
            ]
        (is (= (:status response) (:unprocessable-entity status-code)))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    ))
