(defproject clojure-datomic-backend-sample "0.1.0-SNAPSHOT"
  :description "Sample of a REST backend on Datomic written in Clojure."
  :url "https://github.com/ivos/clojure-datomic-backend-sample"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-free "0.9.5359"]
                 [io.rkn/conformity "0.4.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.5.0"]
                 [bouncer "1.0.0"]
                 [slingshot "0.12.2"]
                 [clj-time "0.12.0"]
                 [commons-codec/commons-codec "1.10"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 ;[midje "1.8.3"]
                 [ring/ring-mock "0.3.0"]
                 ]
  :main ^:skip-aot backend.app
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.8.3"]]
                   :plugins [[lein-midje "3.2"]]}}
  :plugins [[lein-ring "0.9.7"]
            [lein-uberwar "0.2.0"]]
  :ring {:handler backend.app/repl-handler}
  :uberwar {:handler backend.app/repl-handler}
  )
