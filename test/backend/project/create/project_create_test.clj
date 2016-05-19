(ns backend.project.create.project-create-test
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [ring.mock.request :as mock]
            [cheshire.core :refer [encode decode]]
            [datomic.api :as d]
            [backend.db :refer :all]
            [backend.router :refer :all]
            [backend.app :refer [config]]))

(defn new-config
  [db-uri]
  (assoc-in config [:db :uri] db-uri))

(defn reformat
  [json]
  (-> json decode encode))

(deftest project-create-test
  (testing
    "Full"
    (let [db-uri (str "datomic:mem://" (java.util.UUID/randomUUID))
          config (new-config db-uri)
          handler (create-handler config)
          request-body (slurp "test/backend/project/create/full-request.json")
          verify (-> "test/backend/project/create/full-verify.edn" slurp edn/read-string)
          request (-> (mock/request :post "/projects" request-body)
                    (mock/content-type "application/json"))
          _ (start-database! db-uri)
          response (handler request)
          location (get-in response [:headers "Location"])
          id (-> location (.split "/") last Long.)
          db (-> db-uri d/connect d/db)
          created (d/pull db '[* {:project/visibility [:db/ident]}] id)
          ]
      ;        (clojure.pprint/pprint response)
      (is (= (:status response) 201))
      (is (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8"))
      (is (= (get-in response [:headers "ETag"]) "1"))
      (is (= (:body response) (reformat request-body)))
      (is (.startsWith location (get-in config [:app :deploy-url])))
      ;        (clojure.pprint/pprint created)
      (is (= (assoc verify :db/id id) created))
      )))
