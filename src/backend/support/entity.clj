(ns backend.support.entity
  (:require [clojure.tools.logging :as log]
            [datomic.api :as d]
            [slingshot.slingshot :refer [throw+]]
            ))

(defn empty-strings-to-nils
  "Convert empty string values to nils."
  [data]
  (let [empty-string-to-nil #(if (and (string? %) (empty? %)) nil %)]
    (zipmap (keys data) (map empty-string-to-nil (vals data)))))

; TODO remove if unused:
(defn remove-nil-values
  "Remove mappings with nil values."
  [data]
  (into {} (remove (comp nil? val) data)))

(defn ns-value
  [data attribute ns]
  (if-some [value (get data attribute)]
    (let [namespaced (keyword (name ns) value)]
      (assoc data attribute namespaced))
    data))

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
      (nil? value) [:db/retract (:eid data) attribute db-value]
      :otherwise [:db/add (:eid data) attribute value])))

(defn- get-version-number
  [db-data version]
  (try (Long. version)
    (catch NumberFormatException _
      (throw+ {:type :optimistic-locking-failure
               :v (:entity/version db-data)}))))

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
        version-number (get-version-number db-data version)
        eid (:eid data)
        tx (conj
             filtered
             [:db/add eid :entity/version (inc version-number)]
             [:optimistic-lock eid version-number])]
    (log/trace "Update tx" tx)
    tx))

(defn entity-delete-tx
  [db-data eid version]
  {:pre [(integer? eid) (or (string? version) (integer? version))]}
  (let [version-number (get-version-number db-data version)
        tx [[:optimistic-lock eid version-number]
            [:db.fn/retractEntity eid]]
        ]
    (log/trace "Delete tx" tx)
    tx))
