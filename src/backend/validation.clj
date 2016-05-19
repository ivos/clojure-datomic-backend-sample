(ns backend.validation
  (:require [clojure.tools.logging :as log]
            [bouncer.core :as b]
            ))

(defn- validation-message-fn
  "Customize validation message codes."
  [{:keys [path value metadata]}]
  (cond
    (= :bouncer.validators/required (:validator metadata)) "required"
    (= :bouncer.validators/member (:validator metadata)) "invalid.enum.value"
    ))

; Create custom ValidationException to enable specific catch clause and wrap validation errors
(gen-class
  :name backend.validation.ValidationException
  :extends java.lang.RuntimeException
  :prefix validation-exception-
  :init init
  :state state
  :constructors {[Object] [String]}
  :methods [[getValidationErrors [] Object]])
(defn- validation-exception-init
  [validationErrors]
  [[(str "Validation failure:" validationErrors)] validationErrors])
(defn- validation-exception-getValidationErrors
  [this]
  (.state this))

(defn validate!
  "Throw ValidationException on validation failure."
  [& args]
  (when-let [errors (first (apply b/validate (cons validation-message-fn args)))]
    (log/debug "Validation failure" errors)
    (throw (backend.validation.ValidationException. errors))))

(defn wrap-validation
  "Ring middleware to catch ValidationException and convert it to HTTP 422 response."
  [handler]
  (fn
    [request]
    (try (handler request)
      (catch backend.validation.ValidationException e
        (let [response {:status 422
                        :body (.getValidationErrors e)}]
          (log/info "Returning validation failure response" response)
          response)))))
