(ns backend.router
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
;            [ring.middleware.params :refer :all]
;            [ring.middleware.keyword-params :refer :all]
            [datomic.api :as d]
            [backend.config :refer :all]
            [backend.validation :refer [wrap-validation]]
            [backend.project :refer :all]
            ))

(defroutes route-handler
  (GET "/" [] "<h1>Hello compojure</h1>")
  (POST "/projects" request (project-create request))
  (route/not-found "Page not found"))

(defn wrap-connection
  [handler]
  (fn
    [request]
    (let [uri (:uri db-config)
          conn (d/connect uri)
          request-wrapped (assoc request :connection conn)]
      (handler request-wrapped))))

(defn wrap-log
  [handler]
  (fn
    [request]
    (let [request-info
          (clojure.string/join
            " "
            [(:protocol request)
             (-> request :request-method name clojure.string/upper-case)
             (:uri request)])]
      (log/info ">>> Request"
                request-info
                (:body request))
      (let [response (handler request)]
        (log/info "<<< Response" request-info response)
        response))))

(def handler
  (-> route-handler
    wrap-validation
    wrap-log
    (wrap-json-body json-config)
    wrap-json-response
;    wrap-keyword-params
;    wrap-params
    wrap-connection
    ))

(defn start-router!
  "Start HTTP server."
  [options]
  (run-jetty handler options))
