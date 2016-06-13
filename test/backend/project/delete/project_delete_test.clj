(ns backend.project.delete.project-delete-test
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
        (-> (mock/request :delete (str "/projects/" id))
          (mock/content-type "application/json"))]
    (if (nil? version)
      request
      (mock/header request "If-Match" version))))

(facts
  "Project delete"
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/delete/setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        ]
    (fact
      "Full"
      (let [request (create-request "code-full" 123)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            eid (maybe-eid db-after :entity.type/project :project/code "code-full")
            ]
        (fact "Status"
              (:status response) => (:no-content status-code))
        (fact "Deleted"
              eid => nil)
        ))
    (fact
      "Not found"
      (not-found-test handler (create-request 10 123)))
    (fact
      "Optimistic lock failure"
      (let [verify (read-edn "backend/project/delete/optimistic-verify")
            request (create-request "code-optimistic" 122)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/project :project/code "code-optimistic")
            entity (get-entity db eid)
            ]
        (is-response-conflict response 123) ; TODO switch to precondition-failed?
        (fact "Verify"
              (dissoc entity :eid) => verify)
        ))
    (fact
      "Version missing"
      (let [verify (read-edn "backend/project/delete/optimistic-verify")
            request (create-request "code-optimistic" nil)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            eid (get-eid db :entity.type/project :project/code "code-optimistic")
            entity (get-entity db eid)
            ]
        (is-response-precondition-required response)
        (fact "Verify"
              (dissoc entity :eid) => verify)
        ))
    ))
