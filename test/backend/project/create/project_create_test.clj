(ns backend.project.create.project-create-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.db :refer :all]
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
            id (-> location (.split "/") last Long.)
            db (-> db-uri d/connect d/db)
            created (d/pull db '[* {:project/visibility [:db/ident]
                                    :entity/type [:db/ident]}]
                            id)
            ]
        ;(clojure.pprint/pprint response)
        (is-response-created response request-body config)
        ;(clojure.pprint/pprint created)
        (is (= (assoc verify :db/id id) created))
        ))
    (testing
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/project/create/empty-response")
            request (create-request request-body)
            response (handler request)
            ]
        (is (= (:status response) 422))
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
        (is (= (:status response) 422))
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
        (is (= (:status response) 422))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    ))
