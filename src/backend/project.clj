(ns backend.project
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            [ring.util.response :refer :all]
            [bouncer.validators :as v]
            [backend.ring :refer :all]
            [backend.entity :refer :all]
            [backend.datomic :refer :all]
            [backend.validation :refer [verify-keys! validate!]]
            ))

(def ^:private db-partition :db.part/backend)
(def ^:private attributes [:project/name :project/code :project/visibility])
(def ^:private visibilities #{:project.visibility/public :project.visibility/private})

(defn- get-request-data
  [request id]
  (let [data (-> (:body request)
               (ns-keys attributes)
               (ns-value :project/visibility :project.visibility)
               (assoc :id id))]
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
  (verify-keys! (conj attributes :id) data)
  (validate! data
             :project/name v/required
             :project/code v/required
             :project/visibility [[v/required] [v/member visibilities]]))

(defn- get-result
  [data]
  (-> data
    (dissoc :id :entity/version :entity/type)
    (strip-value-ns :project/visibility)
    strip-keys-ns))

(defn- get-detail-uri
  [request id]
  (str (get-in request [:config :app :deploy-url]) "projects/" id))

; public functions

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
        id (d/resolve-tempid db-after (:tempids tx-result) tempid)
        saved (get-entity db-after id)
        _ (log/debug "Saved" saved)
        result (get-result saved)
        response (-> (created (get-detail-uri request id) result)
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
                    :in $ ?type ?name-param ?code-param ?visibility-param
                    :where [?e :entity/type ?type]
                    [?e :project/name ?name] [(backend.datomic/query-string ?name ?name-param)]
                    [?e :project/code ?code] [(backend.datomic/query-string ?code ?code-param)]
                    [?e :project/visibility ?visibility] [(backend.datomic/query-keyword ?visibility ?visibility-param)]
                    ]
                  db :entity.type/project
                  (:project/name query) (:project/code query) (:project/visibility query))
        data (map #(get-entity db (first %)) eids)
        sorted (sort-by (juxt
                          (comp string/lower-case :project/name)
                          (comp string/lower-case :project/code))
                        data)
        to-result #(-> %
                     get-result
                     (assoc :uri (get-detail-uri request (:id %))))
        result (map to-result sorted)
        ]
    (response result)))

(defn project-read
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id Long.)
        eid (get-eid db id :entity.type/project)
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
        id (-> request :params :id Long.)
        data (get-request-data request id)
        version (get-if-match request)
        _ (log/debug "Updating" data)
        _ (validate-common! data)
        eid (get-eid db id :entity.type/project)
        db-data (get-entity db eid)
        _ (log/debug "Read" db-data)
        tx (entity-update-tx db-partition :entity.type/project attributes db-data data version)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        saved (get-entity db-after id)
        _ (log/debug "Saved" saved)
        result (get-result saved)
        response (-> (response result)
                   (header-etag saved))]
    response))

(defn project-delete
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id Long.)
        version (get-if-match request)
        eid (get-eid db id :entity.type/project)
        db-data (get-entity db eid)
        _ (log/debug "Read" db-data)
        tx (entity-delete-tx db-data id version)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        response {:status (status-code :no-content)}]
    response))
