(ns backend.project.delete.project-delete-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.support.db :refer :all]
            [backend.router :refer :all]
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

(deftest project-delete-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/delete/full-setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-full")
                 ffirst)]
    (testing
      "Full"
      (let [request (create-request id 123)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            deleted (->
                      (d/q '[:find ?e :in $ ?e :where [?e]]
                           db-after id)
                      ffirst)
            ]
        ;(clojure.pprint/pprint response)
        (is (= (:status response) 204))
        (is (= nil deleted))
        ))
    (testing
      "Not found"
      (not-found-test handler (create-request 10 123)))
    (testing
      "Optimistic lock failure"
      (let [id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-optimistic")
                 ffirst)
            verify (read-edn "backend/project/delete/optimistic-verify")
            request (create-request id 122)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            deleted (d/pull db-after '[* {:project/visibility [:db/ident]
                                          :entity/type [:db/ident]}]
                            id)
            ]
        (is-response-conflict response 123) ; TODO switch to precondition-failed?
        (is (= (assoc verify :db/id id) deleted))
        ))
    (testing
      "Version missing"
      (let [id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-optimistic")
                 ffirst)
            verify (read-edn "backend/project/delete/optimistic-verify")
            request (create-request id nil)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            deleted (d/pull db-after '[* {:project/visibility [:db/ident]
                                          :entity/type [:db/ident]}]
                            id)
            ]
        (is-response-precondition-required response)
        (is (= (assoc verify :db/id id) deleted))
        ))
    ))
