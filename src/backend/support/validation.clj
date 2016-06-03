(ns backend.support.validation
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]
            [bouncer.core :as b]
            [slingshot.slingshot :refer [throw+ try+]]
            [backend.support.ring :refer [status-code]]
            ))

(defn- validation-message-fn
  "Customize validation message codes."
  [{:keys [path value metadata]}]
  (cond
    (= :bouncer.validators/required (:validator metadata)) "required"
    (= :bouncer.validators/member (:validator metadata)) "invalid.enum.value"
    ))

(defn verify-keys!
  "Throw ValidationException on unknown data key."
  [attributes data]
  (let [keys (set/difference (set (keys data)) (set attributes))
        errors (zipmap keys (repeat ["invalid.attribute"]))]
    (when (not (empty? keys))
               (log/debug "Validation failure, unknown keys" keys)
               (throw+ {:type ::validation-failure :errors errors})
               )))

(defn validate!
  "Throw ValidationException on validation failure."
  [& args]
  (when-let [errors (first (apply b/validate (cons validation-message-fn args)))]
    (log/debug "Validation failure" errors)
    (throw+ {:type ::validation-failure :errors errors})))

(defn wrap-validation
  "Ring middleware to catch ValidationException and convert it to
   HTTP 422 Unprocessable Entity response."
  [handler]
  (fn
    [request]
    (try+ (handler request)
      (catch [:type ::validation-failure] {:keys [errors]}
        (let [response {:status (status-code :unprocessable-entity)
                        :body errors}]
          (log/info "Returning validation failure response" response)
          response)))))
