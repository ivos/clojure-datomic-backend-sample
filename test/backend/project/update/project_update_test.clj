(ns backend.project.update.project-update-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.db :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [id body]
  (-> (mock/request :put (str "/projects/" id) body)
    (mock/content-type "application/json")
    (mock/header "If-Match" 123)))

(deftest project-update-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/update/full-setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))
        id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-full")
                 ffirst)]
    (testing
      "Full"
      (let [request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/full-verify")
            request (create-request id request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (d/pull db-after '[* {:project/visibility [:db/ident]
                                          :entity/type [:db/ident]}]
                            id)
            ]
        ;(clojure.pprint/pprint response)
        (is-response-ok response request-body 124)
        ;(clojure.pprint/pprint updated)
        (is (= (assoc verify :db/id id) updated))
        ))
    (testing
      "Not found"
      (let [request-body (read-json "backend/project/update/full-request")]
        (not-found-test handler (create-request 10 request-body))))
    (testing
      "Empty"
      (let [request-body "{}"
            response-body (read-json "backend/project/update/empty-response")
            request (create-request id request-body)
            response (handler request)
            ]
        (is (= (:status response) 422))
        (is-response-json response)
        (is (= (:body response) response-body))
        ))
    (testing
      "Optimistic lock failure"
      (let [id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-optimistic")
                 ffirst)
            request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/optimistic-verify")
            request (mock/header (create-request id request-body) "If-Match" 122)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (d/pull db-after '[* {:project/visibility [:db/ident]
                                          :entity/type [:db/ident]}]
                            id)
            ]
        (is-response-conflict response 123) ; TODO switch to precondition-failed?
        (is (= (assoc verify :db/id id) updated))
        ))
    (testing
      "Version missing"
      (let [id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-optimistic")
                 ffirst)
            request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/optimistic-verify")
            request (update-in (create-request id request-body) [:headers] dissoc "if-match")
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (d/pull db-after '[* {:project/visibility [:db/ident]
                                          :entity/type [:db/ident]}]
                            id)
            ]
        (is-response-precondition-required response)
        (is (= (assoc verify :db/id id) updated))
        ))
    ))
