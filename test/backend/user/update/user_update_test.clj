(ns backend.user.update.user-update-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.support.datomic :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [id version body]
  (let [request 
        (-> (mock/request :put (str "/users/" id) body)
          (mock/content-type "application/json"))]
    (if (nil? version)
      request
      (mock/header request "If-Match" version))))

(deftest user-update-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/user/update/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        ]
    (testing
      "Full"
      (let [eid (get-eid db :entity.type/user :user/username "username-full")
            request-body (read-json "backend/user/update/full-request")
            response-body (read-json "backend/user/update/full-response")
            verify (read-edn "backend/user/update/full-verify")
            request (create-request "username-full" 123 request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-ok-version response response-body 124)
        (is (= verify (dissoc updated :eid)))
        (is (= id "username-full-a"))
        ))
    (testing
      "Minimal"
      (let [eid (get-eid db :entity.type/user :user/username "username-minimal")
            request-body (read-json "backend/user/update/minimal-request")
            response-body (read-json "backend/user/update/minimal-response")
            verify (read-edn "backend/user/update/minimal-verify")
            request (create-request "username-minimal" 123 request-body)
            response (handler request)
            location (get-in response [:headers "Location"])
            id (-> location (.split "/") last)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-ok-version response response-body 124)
        (is (= verify (dissoc updated :eid)))
        (is (= id "username-minimal-a"))
        ))
    (testing
      "Not found"
      (let [request-body (read-json "backend/user/update/full-request")]
        (not-found-test handler (create-request "non-existent" 123 request-body))))
    (testing
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/user/update/empty-response")
            request (create-request "username-optimistic" 123 request-body)
            response (handler request)
            ]
        (is (= (:status response) 422))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    (testing
      "Optimistic lock failure"
      (let [eid (get-eid db :entity.type/user :user/username "username-optimistic")
            request-body (read-json "backend/user/update/full-request")
            verify (read-edn "backend/user/update/optimistic-verify")
            request (create-request "username-optimistic" 122 request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-conflict response 123) ; TODO switch to precondition-failed?
        (is (= verify (dissoc updated :eid)))
        ))
    (testing
      "Version missing"
      (let [eid (get-eid db :entity.type/user :user/username "username-optimistic")
            request-body (read-json "backend/user/update/full-request")
            verify (read-edn "backend/user/update/optimistic-verify")
            request (create-request "username-optimistic" nil request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (get-entity db-after eid)
            ]
        (is-response-precondition-required response)
        (is (= verify (dissoc updated :eid)))
        ))
    ))
