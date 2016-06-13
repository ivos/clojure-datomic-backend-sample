(ns backend.rest.misc.rest-misc-test
  (:require [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.support.ring :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
            [backend.test-support :refer :all]
            ))

(facts
  "Method not allowed"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        ;setup (read-edn "backend/project/read/full-setup")
        ;db (:db-after @(d/transact (d/connect db-uri) setup))
        ]
    ; TODO uncomment
    '(facts
       "Invalid HTTP method"
       (let [response (handler (mock/request :patch (str "/projects/" 10)))
             ]
         (clojure.pprint/pprint response)
         (fact "Status code"
               (:status response) => (status-code :created))
         (is (= (:status response) :method-not-allowed))
         (is (= (get-in response [:headers "Allow"]) "HEAD OPTIONS GET"))
         ))
    ))

(facts
  "Invalid enum value create"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)]
    (start-database! db-uri)
    (facts
      "Invalid enum value in create"
      (let [request-body (read-json "backend/rest/misc/invalid-enum-create-request")
            response-body (read-json "backend/rest/misc/invalid-enum-create-response")
            request (-> (mock/request :post "/projects" request-body)
                      (mock/content-type "application/json"))
            response (handler request)
            ]
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response"
              (:body response) => response-body)
        ))
    ))

(facts
  "Invalid attribute create"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)]
    (start-database! db-uri)
    (facts
      "Invalid attribute in create"
      (let [request-body (read-json "backend/rest/misc/invalid-attribute-create-request")
            response-body (read-json "backend/rest/misc/invalid-attribute-response")
            request (-> (mock/request :post "/projects" request-body)
                      (mock/content-type "application/json"))
            response (handler request)
            ]
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response"
              (:body response) => response-body)
        ))
    ))

(facts
  "Invalid list"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/rest/misc/invalid-list-setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (facts
      "Invalid enum value in list"
      (let [params {:visibility "invalid"}
            request (mock/request :get "/projects" params)
            response (handler request)
            ]
        (is-response-ok response "[]")
        ))
    (facts
      "Invalid attribute in list"
      (let [params {:invalidAttribute "some-value"}
            request (mock/request :get "/projects" params)
            response (handler request)
            response-body (read-json "backend/rest/misc/invalid-attribute-response")
            ]
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response"
              (:body response) => response-body)
        ))
    ))
