(ns backend.user.delete.user-delete-test
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
  [id version]
  (let [request 
        (-> (mock/request :delete (str "/users/" id))
          (mock/content-type "application/json"))]
    (if (nil? version)
      request
      (mock/header request "If-Match" version))))

(facts
  "User delete"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/user/delete/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        ]
    (facts
      "Full"
      (let [request (create-request "username-full" 123)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            eid (maybe-eid db-after :entity.type/user :user/username "username-full")
            ]
        (fact "Status code"
              (:status response) => (status-code :no-content))
        (fact "Id"
              eid => nil)
        ))
    (facts
      "Not found"
      (not-found-test handler (create-request 10 123)))
    (facts
      "Optimistic lock failure"
      (let [verify (read-edn "backend/user/delete/optimistic-verify")
            request (create-request "username-optimistic" 122)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/user :user/username "username-optimistic")
            entity (get-entity db eid)
            ]
        (is-response-conflict response 123) ; TODO switch to precondition-failed?
        (fact "Verify"
              (dissoc entity :eid) => verify)
        ))
    (facts
      "Version missing"
      (let [verify (read-edn "backend/user/delete/optimistic-verify")
            request (create-request "username-optimistic" nil)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/user :user/username "username-optimistic")
            entity (get-entity db eid)
            ]
        (is-response-precondition-required response)
        (fact "Verify"
              (dissoc entity :eid) => verify)
        ))
    ))
