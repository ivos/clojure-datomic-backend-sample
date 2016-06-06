(ns backend.user.create.user-create-test
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
  (-> (mock/request :post "/users" body)
    (mock/content-type "application/json")))

(deftest user-create-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)]
    (start-database! db-uri)
    (testing
      "Full"
      (let [request-body (read-json "backend/user/create/full-request")
            response-body (read-json "backend/user/create/full-response")
            verify (read-edn "backend/user/create/full-verify")
            request (create-request request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/user :user/username id)
            created (get-entity db eid)
            ]
        (is-response-created response response-body config)
        (is (= verify (dissoc created :eid)))
        (is (= id "username-full"))
        ))
    (testing
      "Minimal"
      (let [request-body (read-json "backend/user/create/minimal-request")
            response-body (read-json "backend/user/create/minimal-response")
            verify (read-edn "backend/user/create/minimal-verify")
            request (create-request request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/user :user/username id)
            created (get-entity db eid)
            ]
        (is-response-created response response-body config)
        (is (= verify (dissoc created :eid)))
        (is (= id "username-minimal"))
        ))
    (testing
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/user/create/empty-response")
            request (create-request request-body)
            response (handler request)
            ]
        (is (= (:status response) (:unprocessable-entity status-code)))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    ))
