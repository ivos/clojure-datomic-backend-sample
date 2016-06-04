(ns backend.project.create.project-create-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.support.datomic :refer :all]
            [backend.support.ring :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [body]
  (-> (mock/request :post "/projects" body)
    (mock/content-type "application/json")))

(deftest project-create-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)]
    (start-database! db-uri)
    (testing
      "Full"
      (let [request-body (read-json "backend/project/create/full-request")
            verify (read-edn "backend/project/create/full-verify")
            request (create-request request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/project :project/code id)
            created (get-entity db eid)
            ]
        (is-response-created response request-body config)
        (is (= verify (dissoc created :eid)))
        (is (= id "code-1"))
        ))
    (testing
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/project/create/empty-response")
            request (create-request request-body)
            response (handler request)
            ]
        (is (= (:status response) (:unprocessable-entity status-code)))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    (testing
      "Invalid enum value"
      (let [request-body (read-json "backend/project/create/invalid-enum-request")
            response-body (read-json "backend/project/create/invalid-enum-response")
            request (create-request request-body)
            response (handler request)
            ]
        (is (= (:status response) (:unprocessable-entity status-code)))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    (testing
      "Invalid attribute"
      (let [request-body (read-json "backend/project/create/invalid-attribute-request")
            response-body (read-json "backend/project/create/invalid-attribute-response")
            request (create-request request-body)
            response (handler request)
            ]
        (is (= (:status response) (:unprocessable-entity status-code)))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    ))
