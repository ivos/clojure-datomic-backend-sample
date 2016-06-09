(ns backend.logic.project
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            [ring.util.response :refer :all]
            [bouncer.validators :as v]
            [backend.support.ring :refer :all]
            [backend.support.entity :refer :all]
            [backend.support.datomic :refer :all]
            [backend.support.validation :refer [verify-keys! validate!]]
            ))

(def ^:private db-partition :db.part/backend)
(def ^:private attributes [:project/code :project/name :project/visibility])
(def ^:private visibilities #{:project.visibility/public :project.visibility/private})

(defn- get-request-data
  [request eid]
  (let [data (-> (:body request)
               (ns-keys attributes)
               (ns-value :project/visibility :project.visibility)
               (assoc :eid eid))]
    (log/debug "Request data" data)
    data))

(defn- get-request-query
  [request db]
  (let [query (-> (:params request)
                empty-strings-to-nils
                (ns-keys attributes)
                (ns-value :project/visibility :project.visibility)
                (prepare-query-params attributes db))]
    (log/debug "Request query" query)
    query))

(defn- validate-common!
  [data]
  (verify-keys! (conj attributes :eid) data)
  (validate! data
             :project/code v/required
             :project/name v/required
             :project/visibility [[v/required] [v/member visibilities]]
             ))

(defn- get-result
  [data]
  (-> data
    (dissoc :eid :entity/version :entity/type)
    (strip-value-ns :project/visibility)
    strip-keys-ns))

(defn- get-detail-uri
  [request data]
  (str (get-in request [:config :app :deploy-url]) "projects/" (:project/code data)))

; action functions

(defn project-create
  [request]
  (let [conn (:connection request)
        tempid (d/tempid db-partition -1)
        data (get-request-data request tempid)
        _ (validate-common! data)
        tx (entity-create-tx db-partition :entity.type/project attributes data)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        eid (d/resolve-tempid db-after (:tempids tx-result) tempid)
        saved (get-entity db-after eid)
        _ (log/debug "Saved" saved)
        result (get-result saved)
        response (-> (created (get-detail-uri request saved) result)
                   (header-etag saved))]
    response))

(defn project-list
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        query (get-request-query request db)
        _ (log/debug "Listing" query)
        _ (verify-keys! attributes query)
        eids (d/q '[:find ?e
                    :in $ ?type ?code-param ?name-param ?visibility-param
                    :where [?e :entity/type ?type]
                    [?e :project/code ?code] [(backend.support.datomic/query-string ?code ?code-param)]
                    [?e :project/name ?name] [(backend.support.datomic/query-string ?name ?name-param)]
                    [?e :project/visibility ?visibility] [(backend.support.datomic/query-keyword ?visibility ?visibility-param)]
                    ]
                  db :entity.type/project
                  (:project/code query) (:project/name query) (:project/visibility query))
        data (map #(get-entity db (first %)) eids)
        sorted (sort-by (juxt
                          (comp string/lower-case :project/name)
                          (comp string/lower-case :project/code))
                        data)
        to-result #(-> %
                     (assoc :uri (get-detail-uri request %))
                     get-result)
        result (map to-result sorted)
        ]
    (response result)))

(defn project-read
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id)
        eid (get-eid db :entity.type/project :project/code id)
        data (get-entity db eid)
        _ (log/debug "Read" data)
        result (get-result data)
        response (-> (response result)
                   (header-etag data))]
    response))

(defn project-update
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id)
        version (get-if-match request)
        eid (get-eid db :entity.type/project :project/code id)
        data (get-request-data request eid)
        _ (log/debug "Updating" data)
        _ (validate-common! data)
        db-data (get-entity db eid)
        _ (log/debug "Read" db-data)
        tx (entity-update-tx db-partition :entity.type/project attributes db-data data version)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        saved (get-entity db-after eid)
        _ (log/debug "Saved" saved)
        result (get-result saved)
        response (-> (response result)
                   (header "Location" (get-detail-uri request saved))
                   (header-etag saved))]
    response))

(defn project-delete
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id)
        version (get-if-match request)
        eid (get-eid db :entity.type/project :project/code id)
        db-data (get-entity db eid)
        _ (log/debug "Read" db-data)
        tx (entity-delete-tx db-data eid version)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        response {:status (status-code :no-content)}]
    response))
