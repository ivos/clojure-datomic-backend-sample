(ns backend.logic.session
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [datomic.api :as d]
            [ring.util.response :refer :all]
            [bouncer.validators :as v]
            [slingshot.slingshot :refer [throw+]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [backend.support.ring :refer :all]
            [backend.support.entity :refer :all]
            [backend.support.datomic :refer :all]
            [backend.support.validation :refer [verify-keys! validate!]]
            [backend.logic.user :refer :all]
            ))

(def ^:private db-partition :db.part/backend)
(def ^:private attributes [:session/token :session/created :session/timeToLive :session/expires :session/user])
(def ^:private request-attributes [:user/username])

(defn- get-request-data
  [request]
  (let [data (-> (:body request)
               (ns-keys request-attributes))]
    (log/debug "Request data" (filter-password data))
    data))

(defn- get-result
  [data]
  (-> data
    (dissoc :eid :entity/version :entity/type)
    (ensure-all-attributes attributes)
    strip-keys-ns))

(defn- get-login-user-eid
  [data db]
  (let [eid (-> (d/q '[:find ?e
                       :in $ ?login ?passwordHash
                       :where
                       [?e :entity/type :entity.type/user]
                       (or [?e :user/username ?login]
                           [?e :user/email ?login])
                       [?e :user/passwordHash ?passwordHash]
                       ]
                     db (:user/username data) (hash-password (:password data)))
              ffirst)]
    (if (nil? eid)
      (throw+ {:type :custom-response
               :response {:status (status-code :unauthorized)
                          :body {:code :user.authentication.failure}}})
      eid)))

; action functions

(defn session-create
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        tempid (d/tempid db-partition -1)
        request-data (get-request-data request)
        _ (verify-keys! [:user/username :password] request-data)
        _ (validate! request-data
                     :user/username v/required
                     :password v/required
                     )
        user-eid (get-login-user-eid request-data db)
        now (t/now)
        time-to-live 30
        data {
              :eid tempid
              :session/token (d/squuid)
              :session/created (tc/to-date now)
              :session/timeToLive time-to-live
              :session/expires (tc/to-date (t/plus now (t/minutes time-to-live)))
              :session/user user-eid
              }
        tx (entity-create-tx db-partition :entity.type/session attributes data)
        tx-result @(d/transact conn tx)
        _ (log/trace "Tx result" tx-result)
        db-after (:db-after tx-result)
        eid (d/resolve-tempid db-after (:tempids tx-result) tempid)
        saved (-> (get-entity db-after eid)
                (expand-entity db-after :session/user))
        _ (log/debug "Saved" saved)
        result (-> saved
                 (update :session/user user-get-result)
                 get-result)
        response (-> (response result)
                   (status (:created status-code)))]
    response))

(defn session-list-active
  [request]
  (let [conn (:connection request)
        db (d/db conn)
        eids (d/q '[:find ?e
                    :in $ ?type ?expires-param
                    :where [?e :entity/type ?type]
                    [?e :session/expires ?expires] [(>= ?expires ?expires-param)]
                    ]
                  db :entity.type/session (java.util.Date.))
        data (map #(get-entity db (first %)) eids)
        sorted (sort (compare-by :session/expires descending
                                 :session/created descending
                                 :session/token ascending)
                     data)
        result (map get-result sorted)]
    (response result)))
