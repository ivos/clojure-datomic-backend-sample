(ns backend.entity
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            ))

(defn ns-value
  [data attribute ns]
  (when-some [value (get data attribute)]
    (let [namespaced (keyword (name ns) value)]
      (assoc data attribute namespaced))))

(defn strip-value-ns
  [data attribute]
  (when-some [value (get data attribute)]
    (->> value
      name
      (assoc data attribute))))

(defn ns-keys
  [data attributes]
  (let [name->attr (zipmap (map name attributes) attributes)
        map-key #(if-some [mapped (-> % name name->attr)] mapped %)
        key-attrs (map map-key (keys data))]
    (zipmap key-attrs (vals data))))

(defn strip-keys-ns
  [data]
  (zipmap (map name (keys data)) (vals data)))

(defn- attribute-tx
  [db-data data attribute]
  (let [db-value (get db-data attribute)
        value (get data attribute)]
    (cond
      (= db-value value) nil
      (nil? value) [:db/retract (:id data) attribute db-value]
      :otherwise [:db/add (:id data) attribute value])))

(defn entity-create-tx
  [db-partition type attributes data]
  {:pre [(vector? attributes) (map? data)]}
  (let [data-preset (assoc data :entity/type type :entity/version 1)
        extended-attrs (conj attributes :entity/version :entity/type)
        add-txs (map (partial attribute-tx nil data-preset) extended-attrs)
        tx (filter identity add-txs)]
    (log/trace "Create tx" tx)
    tx))

(defn entity-update-tx
  [db-partition type attributes db-data data version]
  {:pre [(vector? attributes) (map? db-data) (map? data)
         (or (string? version) (integer? version))]}
  (let [update-txs (map (partial attribute-tx db-data data) attributes)
        filtered (filter identity update-txs)
        version-number (Long. version)
        tx (conj
             filtered
             [:db.fn/cas (:id data) :entity/version version-number (inc version-number)])]
    (log/trace "Update tx" tx)
    tx))
