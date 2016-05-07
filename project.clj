(defproject clojure-datomic-backend-sample "0.1.0-SNAPSHOT"
  :description "Sample of a REST backend on Datomic written in Clojure."
  :url "https://github.com/ivos/clojure-datomic-backend-sample"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.datomic/datomic-free "0.9.5359"]
                 [io.rkn/conformity "0.4.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 ]
  :main ^:skip-aot backend.app
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  )
