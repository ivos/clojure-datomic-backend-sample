(ns backend.project.read.project-read-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [datomic.api :as d]
            [backend.db :refer :all]
            [backend.router :refer :all]
            [backend.test-support :refer :all]
            ))

(defn- create-request
  [id]
  (mock/request :get (str "/projects/" id)))

(deftest project-read-test
  (let [db-uri (test-db-uri)
        config (test-config db-uri)
        handler (create-handler config)
        _ (start-database! db-uri)
        setup (read-edn "backend/project/read/full-setup")
        db (:db-after @(d/transact (d/connect db-uri) setup))]
    (testing
      "Full"
      (let [id (-> (d/q '[:find ?e
                          :in $ ?code
                          :where [?e :project/code ?code]]
                        db
                        "code-1")
                 ffirst)
            request (create-request id)
            response (handler request)
            response-body (read-json "backend/project/read/full-response")
            ]
        ;(clojure.pprint/pprint response)
        (is-response-ok response response-body 123)
        ))
    (testing
      "Not found"
      (not-found-test handler (create-request 10)))
    (testing
      "Invalid id"
      (not-found-test handler (create-request "invalid")))
    ))