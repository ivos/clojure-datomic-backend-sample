(ns backend.validation
  (:require [clojure.tools.logging :as log]
            [bouncer.core :as b]
            [slingshot.slingshot :refer [throw+ try+]]
            ))

(defn- validation-message-fn
  "Customize validation message codes."
  [{:keys [path value metadata]}]
  (cond
    (= :bouncer.validators/required (:validator metadata)) "required"
    (= :bouncer.validators/member (:validator metadata)) "invalid.enum.value"
    ))

(defn validate!
  "Throw ValidationException on validation failure."
  [& args]
  (when-let [errors (first (apply b/validate (cons validation-message-fn args)))]
    (log/debug "Validation failure" errors)
    (throw+ {:type ::validation-failure :errors errors})))

(defn wrap-validation
  "Ring middleware to catch ValidationException and convert it to HTTP 422 response."
  [handler]
  (fn
    [request]
    (try+ (handler request)
      (catch [:type ::validation-failure] {:keys [errors]}
        (let [response {:status 422
                        :body errors}]
          (log/info "Returning validation failure response" response)
          response)))))
