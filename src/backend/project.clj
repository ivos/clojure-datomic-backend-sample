(ns backend.project
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            [ring.util.response :refer :all]
            [bouncer.validators :as v]
            [slingshot.slingshot :refer [throw+]]
            [backend.entity :refer :all]
            [backend.datomic :refer :all]
            [backend.validation :refer [verify-keys! validate!]]
            ))

(def ^:private db-partition :db.part/backend)
(def ^:private attributes [:project/name :project/code :project/visibility])
(def ^:private visibilities #{:project.visibility/public :project.visibility/private})

(defn project-create
  [request]
  (let [conn (:connection request)
        tempid (d/tempid db-partition -1)
        data (-> (:body request)
               (ns-keys attributes)
               (ns-value :project/visibility :project.visibility)
               (assoc :id tempid))
        _ (log/debug "Creating" data)
        _ (verify-keys! (conj attributes :id) data)
        _ (validate! data
                     :project/name v/required
                     :project/code v/required
                     :project/visibility [[v/required] [v/member visibilities]])
        tx (entity-create-tx db-partition :entity.type/project attributes data)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        id (d/resolve-tempid db-after (:tempids tx-result) tempid)
        saved (merge {} (d/touch (d/entity db-after id)))
        _ (log/debug "Saved" saved)
        result (-> saved
                 (dissoc :entity/version :entity/type)
                 (strip-value-ns :project/visibility)
                 strip-keys-ns)
        response (-> (str (get-in request [:config :app :deploy-url]) "projects/" id)
                   (created result)
                   (header "ETag" (:entity/version saved)))]
    response))

(defn project-list
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        query (-> (:params request)
                empty-strings-to-nils
                (ns-keys attributes)
                (ns-value :project/visibility :project.visibility)
                (prepare-query-params attributes db)
                )
        _ (log/debug "Listing" query)
        eids (d/q '[:find ?e
                    :in $ ?type ?name-param ?code-param ?visibility-param
                    :where [?e :entity/type ?type]
                    [?e :project/name ?name] [(backend.datomic/query-string ?name ?name-param)]
                    [?e :project/code ?code] [(backend.datomic/query-string ?code ?code-param)]
                    [?e :project/visibility ?visibility] [(backend.datomic/query-keyword ?visibility ?visibility-param)]
                    ]
                  db :entity.type/project (:project/name query) (:project/code query) (:project/visibility query))
        data (map #(merge {:id (first %)} (d/touch (d/entity db (first %)))) eids)
        sorted (sort-by (juxt (comp string/lower-case :project/name) (comp string/lower-case :project/code)) data)
        to-result #(-> %
                     (assoc :uri (str (get-in request [:config :app :deploy-url]) "projects/" (:id %)))
                     (dissoc :id :entity/version :entity/type)
                     (strip-value-ns :project/visibility)
                     strip-keys-ns)
        result (map to-result sorted)
        ]
    (response result)))

(defn project-read
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id Long.)
        eid (-> (d/q '[:find ?e
                       :in $ ?e ?type
                       :where [?e :entity/type ?type]]
                     db id :entity.type/project)
              ffirst)]
    (if eid
      (let [data (merge {} (d/touch (d/entity db eid)))
            _ (log/debug "Read" data)
            result (-> data
                     (dissoc :entity/version :entity/type)
                     (strip-value-ns :project/visibility)
                     strip-keys-ns)
            response (-> (response result)
                       (header "ETag" (:entity/version data)))]
        response)
      (not-found {:code :entity.not.found}))
    ))

(defn project-update
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id Long.)
        data (-> (:body request)
               (ns-keys attributes)
               (ns-value :project/visibility :project.visibility)
               (assoc :id id))
        version (get-in request [:headers "if-match"])
        _ (log/debug "Updating" data)
        _ (verify-keys! (conj attributes :id) data)
        _ (validate! data
                     :project/name v/required
                     :project/code v/required
                     :project/visibility [[v/required] [v/member visibilities]])
        eid (-> (d/q '[:find ?e
                       :in $ ?e ?type
                       :where [?e :entity/type ?type]]
                     db id :entity.type/project)
              ffirst)]
    (when (nil? version) (throw+ {:type :custom-response :response {:status 428}}))
    (if eid
      (let [db-data (merge {} (d/touch (d/entity db eid)))
            _ (log/debug "Read" db-data)
            tx (entity-update-tx db-partition :entity.type/project attributes db-data data version)
            tx-result @(d/transact conn tx)
            _ (log/trace "Tx result" tx-result)
            db-after (:db-after tx-result)
            saved (merge {} (d/touch (d/entity db-after id)))
            _ (log/debug "Saved" saved)
            result (-> saved
                     (dissoc :entity/version :entity/type)
                     (strip-value-ns :project/visibility)
                     strip-keys-ns)
            response (-> (response result)
                       (header "ETag" (:entity/version saved)))]
        response)
      (not-found {:code :entity.not.found}))
    ))

(defn project-delete
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        id (-> request :params :id Long.)
        version (get-in request [:headers "if-match"])
        eid (-> (d/q '[:find ?e
                       :in $ ?e ?type
                       :where [?e :entity/type ?type]]
                     db id :entity.type/project)
              ffirst)]
    (when (nil? version) (throw+ {:type :custom-response :response {:status 428}}))
    (if eid
      (let [db-data (merge {} (d/touch (d/entity db eid)))
            _ (log/debug "Read" db-data)
            tx (entity-delete-tx db-data id version)
            tx-result @(d/transact conn tx)
            _ (log/trace "Tx result" tx-result)
            db-after (:db-after tx-result)
            saved (merge {} (d/touch (d/entity db-after id)))
            result (-> saved
                     (dissoc :entity/version :entity/type)
                     (strip-value-ns :project/visibility)
                     strip-keys-ns)
            response {:status 204}]
        response)
      (not-found {:code :entity.not.found}))
    ))
