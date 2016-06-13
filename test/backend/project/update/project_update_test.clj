(ns backend.project.update.project-update-test
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
  [id version body]
  (let [request 
        (-> (mock/request :put (str "/projects/" id) body)
          (mock/content-type "application/json"))]
    (if (nil? version)
      request
      (mock/header request "If-Match" version))))

(facts
  "Project update"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/update/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        eid (get-eid db :entity.type/project :project/code "code-full")
        ]
    (fact
      "Full"
      (let [request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/full-verify")
            request (create-request "code-full" 123 request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-ok-version response request-body 124)
        (fact "Verify"
              (dissoc updated :eid) => verify)
        (fact "Id"
              id => "code-full-a")
        ))
    (fact
      "Not found"
      (let [request-body (read-json "backend/project/update/full-request")]
        (not-found-test handler (create-request "non-existent" 123 request-body))))
    (fact
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/project/update/empty-response")
            request (create-request "code-optimistic" 123 request-body)
            response (handler request)
            ]
        (fact "Status code"
              (:status response) => (status-code :unprocessable-entity))
        (is-response-json response)
        (fact "Response body"
              (:body response) => response-body)
        ))
    (fact
      "Optimistic lock failure"
      (let [eid (get-eid db :entity.type/project :project/code "code-optimistic")
            request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/optimistic-verify")
            request (create-request "code-optimistic" 122 request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-conflict response 123) ; TODO switch to precondition-failed?
        (fact "Verify"
              (dissoc updated :eid) => verify)
        ))
    (fact
      "Version missing"
      (let [eid (get-eid db :entity.type/project :project/code "code-optimistic")
            request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/optimistic-verify")
            request (create-request "code-optimistic" nil request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-precondition-required response)
        (fact "Verify"
              (dissoc updated :eid) => verify)
        ))
    ))
