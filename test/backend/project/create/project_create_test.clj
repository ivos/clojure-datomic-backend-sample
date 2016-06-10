(ns backend.project.create.project-create-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.support.datomic :refer :all]
            [backend.support.ring :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
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
    (facts
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
    (facts
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/project/create/empty-response")
            request (create-request request-body)
            response (handler request)
            ]
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response body"
              (:body response) => response-body)
        ))
    ))
