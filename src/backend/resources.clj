(ns backend.resources
  (:require [clojure.java.io :as io]))

(def ^:private current-running-jar
  (-> :keyword class (.. getProtectionDomain getCodeSource getLocation getPath)))

(defn- list-all-jar-resources
  [path]
  (let [jar (java.util.jar.JarFile. path)  
        entries (.entries jar)]
    (loop [result []]
      (if (.hasMoreElements entries)
        (recur (conj result (.. entries nextElement getName)))
        result))))

(defn- list-jar-resources
  [path]
  (let [all (list-all-jar-resources current-running-jar)
        sub-path-filter #(and (.startsWith % path)
                              (not (= path %)))
        filtered (filter sub-path-filter all)]
    filtered))

(defn- list-dir-resources
  [path]
  (let [files (-> path (io/resource) (io/file) (.listFiles))
        names (map #(->> % .getName (str path)) files)]
    names))

(defn list-resources
  [path]
  (let [resource (io/resource path)]
    (if (not resource)
      (throw (RuntimeException. (str "Resource not found: " path)))
      (case (.getProtocol resource)
        "jar"  (list-jar-resources path)
        "file" (list-dir-resources path)
        ))
    ))
