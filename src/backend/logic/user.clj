(ns backend.logic.user
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
(def ^:private attributes [:user/username :user/email :user/full-name :user/password-hash])

(defn- get-request-data
  [request eid]
  (let [data (-> (:body request)
               (ns-keys attributes)
               (assoc :eid eid))]
    (log/debug "Request data" (filter-password data))
    data))

(defn- get-request-query
  [request db]
  (let [query (-> (:params request)
                empty-strings-to-nils
                (ns-keys attributes)
                (prepare-query-params attributes db))]
    (log/debug "Request query" query)
    query))

(defn- validate-common!
  [data]
  (verify-keys! (conj attributes :eid) data)
  (validate! data
             :user/username v/required
             :user/email v/required
             :user/password-hash v/required
             ))

(defn- get-result
  [data]
  (-> data
    (dissoc :eid :entity/version :entity/type :user/password-hash)
    strip-keys-ns))

(defn- get-detail-uri
  [request data]
  (str (get-in request [:config :app :deploy-url]) "users/" (:user/username data)))

(defn- hash-password
  [password]
  (let [bytes (.getBytes ^String password)
        digest (-> (java.security.MessageDigest/getInstance "SHA-256")
                 (.digest bytes))
        hash (org.apache.commons.codec.binary.Hex/encodeHexString digest)]
    hash))

(defn- hash-password-data
  [data]
  (-> data
    (assoc :user/password-hash (hash-password (:password data)))
    (dissoc :password)))

; public functions

(defn user-create
  [request]
  (let [conn (:connection request)
        tempid (d/tempid db-partition -1)
        data (get-request-data request tempid)
        _ (verify-keys! (-> attributes
                          set
                          (disj :user/password-hash)
                          (conj :eid :password))
                        data)
        _ (validate! data
                     :user/username v/required
                     :user/email v/required
                     :password v/required
                     )
        data (hash-password-data data)
        tx (entity-create-tx db-partition :entity.type/user attributes data)
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

(defn user-list
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        query (get-request-query request db)
        _ (log/debug "Listing" query)
        _ (verify-keys! attributes query)
        eids (d/q '[:find ?e
                    :in $ ?type ?username-param ?email-param ?full-name-param
                    :where [?e :entity/type ?type]
                    [?e :user/username ?username] [(backend.support.datomic/query-string ?username ?username-param)]
                    [?e :user/email ?email] [(backend.support.datomic/query-string ?email ?email-param)]
                    [?e :user/full-name ?full-name] [(backend.support.datomic/query-string ?full-name ?full-name-param)]
                    ]
                  db :entity.type/user
                  (:user/username query) (:user/email query) (:user/full-name query))
        data (map #(get-entity db (first %)) eids)
        sorted (sort-by (juxt
                          (comp string/lower-case :user/full-name)
                          (comp string/lower-case :user/username))
                        data)
        to-result #(-> %
                     (assoc :uri (get-detail-uri request %))
                     get-result)
        result (map to-result sorted)
        ]
    (response result)))

(defn user-read
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id)
        eid (get-eid db :entity.type/user :user/username id)
        data (get-entity db eid)
        _ (log/debug "Read" data)
        result (get-result data)
        response (-> (response result)
                   (header-etag data))]
    response))

(defn user-update
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id)
        version (get-if-match request)
        eid (get-eid db :entity.type/user :user/username id)
        data (get-request-data request eid)
        _ (log/debug "Updating" (filter-password data))
        _ (validate-common! data)
        db-data (get-entity db eid)
        _ (log/debug "Read" db-data)
        tx (entity-update-tx db-partition :entity.type/user attributes db-data data version)
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

(defn user-delete
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id)
        version (get-if-match request)
        eid (get-eid db :entity.type/user :user/username id)
        db-data (get-entity db eid)
        _ (log/debug "Read" (filter-password db-data))
        tx (entity-delete-tx db-data eid version)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        response {:status (status-code :no-content)}]
    response))
