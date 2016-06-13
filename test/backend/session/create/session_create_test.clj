(ns backend.session.create.session-create-test
  (:import java.util.UUID)
  (:require [ring.mock.request :as mock]
            [datomic.api :as d]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [backend.support.db :refer :all]
            [backend.support.datomic :refer :all]
            [backend.support.ring :refer :all]
            [backend.router :refer :all]
            [midje.sweet :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [body]
  (-> (mock/request :post "/sessions" body)
    (mock/content-type "application/json")))

(facts
  "Session create"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/session/create/setup")
        _ @(d/transact (d/connect db-uri) setup)
        ]
    (start-database! db-uri)
    (facts
      "Full, log in by username"
      (let [request-body (read-json "backend/session/create/full-username-request")
            response-body-expected (read-json "backend/session/create/full-response")
            verify (read-edn "backend/session/create/full-verify")
            request (create-request request-body)
            response (t/do-at std-time (handler request))
            response-body-actual (json/decode (:body response))
            id (UUID/fromString (get response-body-actual "token"))
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db-after :entity.type/session :session/token id)
            created (d/pull db-after
                            '[* {:entity/type [*]} {:session/user [:user/username]}]
                            eid)
            ]
        (fact "Status code"
              (:status response) => (status-code :created))
        (is-response-json response)
        (fact "Response body"
              (-> response-body-actual
                (assoc "token" "5757d606-c0aa-4822-b531-00f479aaa4e9")
                json/encode) => response-body-expected)
        (fact "Verify"
              (-> created
                (update :entity/type :db/ident)
                (assoc :session/token (UUID/fromString "5757d606-c0aa-4822-b531-00f479aaa4e9"))
                (dissoc :db/id)) => verify)
        ))
    (facts
      "Full, log in by email"
      (let [request-body (read-json "backend/session/create/full-email-request")
            response-body-expected (read-json "backend/session/create/full-response")
            verify (read-edn "backend/session/create/full-verify")
            request (create-request request-body)
            response (t/do-at std-time (handler request))
            response-body-actual (json/decode (:body response))
            id (UUID/fromString (get response-body-actual "token"))
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db-after :entity.type/session :session/token id)
            created (d/pull db-after
                            '[* {:entity/type [*]} {:session/user [:user/username]}]
                            eid)
            ]
        (fact "Status code"
              (:status response) => (status-code :created))
        (is-response-json response)
        (fact "Response body"
              (-> response-body-actual
                (assoc "token" "5757d606-c0aa-4822-b531-00f479aaa4e9")
                json/encode) => response-body-expected)
        (fact "Verify"
              (-> created
                (update :entity/type :db/ident)
                (assoc :session/token (UUID/fromString "5757d606-c0aa-4822-b531-00f479aaa4e9"))
                (dissoc :db/id)) => verify)
        ))
    (facts
      "Minimal"
      (let [request-body (read-json "backend/session/create/minimal-request")
            response-body-expected (read-json "backend/session/create/minimal-response")
            verify (read-edn "backend/session/create/minimal-verify")
            request (create-request request-body)
            response (t/do-at std-time (handler request))
            response-body-actual (json/decode (:body response))
            id (UUID/fromString (get response-body-actual "token"))
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db-after :entity.type/session :session/token id)
            created (d/pull db-after
                            '[* {:entity/type [*]} {:session/user [:user/username]}]
                            eid)
            ]
        (fact "Status code"
              (:status response) => (status-code :created))
        (is-response-json response)
        (fact "Response body"
              (-> response-body-actual
                (assoc "token" "5757d606-c0aa-4822-b531-00f479aaa4e9")
                json/encode) => response-body-expected)
        (fact "Verify"
              (-> created
                (update :entity/type :db/ident)
                (assoc :session/token (UUID/fromString "5757d606-c0aa-4822-b531-00f479aaa4e9"))
                (dissoc :db/id)) => verify)
        ))
    (facts
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/session/create/empty-response")
            request (create-request request-body)
            response (handler request)
            ]
         (fact "Status code"
               (:status response) => (status-code :unprocessable-entity))
         (is-response-json response)
         (fact "Response body"
               (:body response) => response-body)
         ))
    (facts
      "User not found"
      (let [request-body (read-json "backend/session/create/user-not-found-request")
            response-body (read-json "backend/session/create/authentication-failure-response")
            request (create-request request-body)
            response (handler request)
            ]
         (fact "Status code"
               (:status response) => (status-code :unauthorized))
          (is-response-json response)
          (fact "Response body"
                (:body response) => response-body)
         ))
    (facts
      "Password mismatch"
      (let [request-body (read-json "backend/session/create/password-mismatch-request")
            response-body (read-json "backend/session/create/authentication-failure-response")
            request (create-request request-body)
            response (handler request)
            ]
         (fact "Status code"
               (:status response) => (status-code :unauthorized))
          (is-response-json response)
          (fact "Response body"
                (:body response) => response-body)
         ))
    ))
