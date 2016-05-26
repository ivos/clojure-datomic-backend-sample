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
    (mock/content-type "application/json")))

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
                        "code-1")
                 ffirst)]
    (testing
      "Full"
      (let [id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-1")
                 ffirst)
            request-body (read-json "backend/project/update/full-request")
            verify (read-edn "backend/project/update/full-verify")
            request (create-request id request-body)
            response (handler request)
            db-after (-> db-uri d/connect d/db)
            updated (d/pull db-after '[* {:project/visibility [:db/ident]
                                          :entity/type [:db/ident]}]
                            id)
            ]
        ;(clojure.pprint/pprint response)
        (is-response-ok response request-body 123)
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
    ))
