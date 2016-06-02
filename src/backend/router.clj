(ns backend.router
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [not-found header]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.params :refer :all]
            [ring.middleware.keyword-params :refer :all]
            [datomic.api :as d]
            [slingshot.slingshot :refer [try+]]
            [backend.validation :refer [wrap-validation]]
            [backend.project :refer :all]
            ))

(defroutes app-handler
  (GET "/" [] "<h1>Hello compojure</h1>")
  (POST "/projects" request (project-create request))
  (GET "/projects" request (project-list request))
  (GET "/projects/:id{[0-9]+}" request (project-read request))
  (PUT "/projects/:id{[0-9]+}" request (project-update request))
  (DELETE "/projects/:id{[0-9]+}" request (project-delete request))
  (route/not-found (fn [_] (not-found {:code :entity.not.found}))))

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

(defn- wrap-conflict
  [handler]
  (fn
    [request]
    (try+ (handler request)
          (catch [:type :optimistic-locking-failure] {:keys [:v]}
            (header {:status 409} "ETag" v)))))

(defn- wrap-custom-response
  [handler]
  (fn
    [request]
    (try+ (handler request)
          (catch [:type :custom-response] {:keys [:response]}
            response))))

(defn- wrap-log
  [handler]
  (fn
    [request]
    (let [request-info
          (clojure.string/join
            " "
            [(:protocol request)
             (-> request :request-method name clojure.string/upper-case)
             (:uri request)])
          body (when-not (#{:get :head :delete} (:request-method request))
                 (:body request))]
      (log/info ">>> Request"
                request-info
                "Params:" (:params request)
                "Body:" body
                "Headers:" (-> request :headers (dissoc "host" "content-length")))
      (let [response (handler request)]
        (log/info "<<< Response" request-info response)
        response))))

(defn create-handler
  [config]
  (-> app-handler
    wrap-validation
    wrap-conflict
    wrap-custom-response
    wrap-log
    (wrap-json-body (:json config))
    wrap-json-response
    wrap-keyword-params
    wrap-params
    (wrap-connection (get-in config [:db :uri]))
    (wrap-config config)
    ))

(defn start-router!
  "Start HTTP server."
  [config]
  (run-jetty (create-handler config) (:jetty config)))
