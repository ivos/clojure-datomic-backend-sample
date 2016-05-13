(ns backend.entity
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            ))

(defn ns-value
  [data attribute ns]
  (let [namespaced (keyword (name ns) (get data attribute))]
    (assoc data attribute namespaced)))

(defn ns-keys
  [data attributes]
  (let [name->attr (zipmap (map name attributes) attributes)
        map-key #(if-some [mapped (-> % name name->attr)] mapped %)
        key-attrs (map map-key (keys data))]
    (zipmap key-attrs (vals data))))

(defn- attribute-add-tx
  [data attribute]
  (when-some [value (get data attribute)]
             [:db/add (:id data) attribute value]))

(defn entity-create-tx
  [db-partition attributes data]
  (let [data-with-defaults (assoc data :id (d/tempid db-partition) :entity/version 1)
        add-txs (map (partial attribute-add-tx data-with-defaults) (conj attributes :entity/version))]
    (filter identity add-txs)))
