(ns backend.router
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer :all]
            [ring.middleware.keyword-params :refer :all]
            [backend.project :refer :all]
            ))

(defroutes route-handler
  (GET "/" [] "<h1>Hello compojure</h1>")
  (POST "/projects" request (project-create request))
  (route/not-found "Page not found"))

(def handler
  (-> route-handler
      (wrap-keyword-params)
      (wrap-params)
      ))

(defn start-router!
  "Start HTTP server."
  [options]
  (run-jetty handler options))
