(ns backend.router
  (:require [compojure.core :refer :all]
            [compojure.route :as route]))

(defroutes handler
  (GET "/" [] "<h1>Hello compojure</h1>")
  (route/not-found "Page not found"))
