(ns backend.user.create.user-create-test
  (:require [ring.mock.request :as mock]
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
  (-> (mock/request :post "/users" body)
    (mock/content-type "application/json")))

(facts
  "User create"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/user/create/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        ]
    (fact
      "Full"
      (let [request-body (read-json "backend/user/create/full-request")
            response-body (read-json "backend/user/create/full-response")
            verify (read-edn "backend/user/create/full-verify")
            request (create-request request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db-after :entity.type/user :user/username id)
            created (get-entity db-after eid)
            ]
        (is-response-created response response-body config)
        (fact "Verify"
              (dissoc created :eid) => verify)
        (fact "Id"
              id => "username-full")
        ))
    (fact
      "Minimal"
      (let [request-body (read-json "backend/user/create/minimal-request")
            response-body (read-json "backend/user/create/minimal-response")
            verify (read-edn "backend/user/create/minimal-verify")
            request (create-request request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db-after :entity.type/user :user/username id)
            created (get-entity db-after eid)
            ]
        (is-response-created response response-body config)
        (fact "Verify"
              (dissoc created :eid) => verify)
        (fact "Id"
              id => "username-minimal")
        ))
    (fact
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/user/create/empty-response")
            request (create-request request-body)
            response (handler request)
            ]
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response body"
              (:body response) => response-body)
        ))
    (fact
      "Username exists"
      (let [request-body (read-json "backend/user/create/username-exists-request")
            response-body (read-json "backend/user/create/username-exists-response")
            verify (read-edn "backend/user/create/existing-verify")
            request (create-request request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            count (-> (d/q '[:find (count ?e)
                         :in $ ?username
                         :where [?e :user/username ?username]]
                       db-after "username-existing")
                    ffirst)
            eid (get-eid db-after :entity.type/user :user/username "username-existing")
            existing (get-entity db-after eid)
            ]
        (fact "No multiple records"
              count => 1)
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response body"
              (:body response) => response-body)
        (fact "Verify"
              (dissoc existing :eid) => verify)
        ))
    (fact
      "Email exists"
      (let [request-body (read-json "backend/user/create/email-exists-request")
            response-body (read-json "backend/user/create/email-exists-response")
            verify (read-edn "backend/user/create/existing-verify")
            request (create-request request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            count (-> (d/q '[:find (count ?e)
                         :in $ ?email
                         :where [?e :user/email ?email]]
                       db-after "email-existing@site.com")
                    ffirst)
            eid (get-eid db-after :entity.type/user :user/username "username-existing")
            existing (get-entity db-after eid)
            ]
        (fact "No multiple records"
              count => 1)
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response body"
              (:body response) => response-body)
        (fact "Verify"
              (dissoc existing :eid) => verify)
        ))
    ))
