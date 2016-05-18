(ns backend.router
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
;            [ring.middleware.params :refer :all]
;            [ring.middleware.keyword-params :refer :all]
            [datomic.api :as d]
            [backend.validation :refer [wrap-validation]]
            [backend.project :refer :all]
            ))

(defroutes route-handler
  (GET "/" [] "<h1>Hello compojure</h1>")
  (POST "/projects" request (project-create request))
  (route/not-found "Page not found"))

(defn- wrap-config
  [handler config]
  (fn
    [request]
    (let [request-wrapped (assoc request :config config)]
      (handler request-wrapped))))

(defn- wrap-connection
  [handler uri]
  (fn
    [request]
    (let [conn (d/connect uri)
          request-wrapped (assoc request :connection conn)]
      (handler request-wrapped))))

(defn- wrap-log
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

(defn handler
  [config]
  (-> route-handler
    wrap-validation
    wrap-log
    (wrap-json-body (:json config))
    wrap-json-response
;    wrap-keyword-params
;    wrap-params
    (wrap-connection (get-in config [:db :uri]))
    (wrap-config config)
    ))

(defn start-router!
  "Start HTTP server."
  [config]
  (run-jetty (handler config) (:jetty config)))
