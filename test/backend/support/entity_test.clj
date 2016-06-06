(ns backend.support.entity-test
  (:require [clojure.test :refer :all]
            [slingshot.slingshot :refer [try+]]
            [backend.support.entity :refer :all]))

(deftest empty-strings-to-nils-test
  (testing
    (let [data {:with-value "abc"
                :empty-string ""
                :number-zero 0
                :false-bool false
                :number-non-zero 123
                :keyword :keyword-value}]
      (is (= (empty-strings-to-nils data)
             {:with-value "abc"
              :empty-string nil
              :number-zero 0
              :false-bool false
              :number-non-zero 123
              :keyword :keyword-value}
             ))))
  )

(deftest remove-nil-values-test
  (testing
    (let [data {:with-value "abc"
                :nil-value1 nil
                :number-zero 0
                :false-bool false
                :nil-value2 nil
                :number-non-zero 123
                :keyword :keyword-value}]
      (is (= (remove-nil-values data)
             {:with-value "abc"
              :number-zero 0
              :false-bool false
              :number-non-zero 123
              :keyword :keyword-value}
             ))))
  )

(deftest ensure-all-attributes-test
  (testing
    (let [data {:with-value "abc"
                :nil-value1 nil
                :false-bool false
                :number-non-zero 123
                :other "other-value"
                :keyword :keyword-value}
          attributes [:with-value :nil-value1 :number-non-zero :missing1 :missing2 :keyword]]
      (is (= (ensure-all-attributes data attributes)
             {:with-value "abc"
              :nil-value1 nil
              :false-bool false
              :number-non-zero 123
              :other "other-value"
              :keyword :keyword-value
              :missing1 nil
              :missing2 nil}
             ))))
  )

(deftest ns-value-test
  (testing
    "Ok."
    (let [data {:a 1 :an-attr "a-value"}]
      (is (= {:a 1 :an-attr :a-ns/a-value}
             (ns-value data :an-attr :a-ns)))))
  (testing
    "Attribute value is nil."
    (let [data {:a 1 :an-attr nil}]
      (is (= {:a 1 :an-attr nil}
             (ns-value data :an-attr :a-ns)))))
  (testing
    "Attribute is not present."
    (let [data {:a 1}]
      (is (= {:a 1}
             (ns-value data :an-attr :a-ns)))))
  )

(deftest ns-keys-test
  (testing
    "Found is replaced."
    (is (= 
          {:my-entity/attr 1}
          (ns-keys {:attr 1} [:my-entity/attr]))))
  (testing
    "Not found is kept intact."
    (is (= 
          {:non-existing 1}
          (ns-keys {:non-existing 1} [:my-entity/attr]))))
  (testing
    "Multiple namespaces."
    (is (= 
          {:my-entity/attr-a 1 :other-entity/attr-b 2}
          (ns-keys {:attr-a 1 :attr-b 2} [:my-entity/attr-a :other-entity/attr-b]))))
  (testing
    "Nil value."
    (is (= 
          {:my-entity/attr nil}
          (ns-keys {:attr nil} [:my-entity/attr]))))
  )

(deftest entity-create-tx-test
  (testing
    (let [attributes [:add1 :add2 :keep-nil3]
          data {:eid :v-eid
                :add1 :v-add1
                :add2 :v-add2
                :other :v-other}
          tx (entity-create-tx :db-part1 :type1 attributes data)
          ]
      (is (=
            '([:db/add :v-eid :add1 :v-add1]
               [:db/add :v-eid :add2 :v-add2]
               [:db/add :v-eid :entity/version 1]
               [:db/add :v-eid :entity/type :type1])
            tx))
      ))
  )

(deftest entity-update-tx-test
  (testing
    (let [attributes [:modify1 :modify2 :retract3 :add4 :keep-same5 :keep-nil6]
          db-data {
                   :modify1 :db-modify1
                   :modify2 :db-modify2
                   :retract3 :db-retract3
                   :keep-same5 :v-keep-same5
                   :keep-nil6 nil}
          data {
                :eid :v-eid
                :modify1 :v-modify1
                :modify2 :v-modify2
                :retract3 nil
                :add4 :v-add4
                :keep-same5 :v-keep-same5
                :keep-nil6 nil
                :other :v-other}
          tx (entity-update-tx :db-part1 :type1 attributes db-data data 123)
          ]
      (is (=
            '([:optimistic-lock :v-eid 123]
               [:db/add :v-eid :entity/version 124]
               [:db/add :v-eid :modify1 :v-modify1]
               [:db/add :v-eid :modify2 :v-modify2]
               [:db/retract :v-eid :retract3 :db-retract3]
               [:db/add :v-eid :add4 :v-add4]
               )
            tx))
      ))
  (testing
    "Invalid version"
    (let [attributes [:a]
          db-data {:a :db-a
                   :entity/version :db-version}
          data {:eid :v-eid
                :a :v-a}
          ]
      (try+ (do
              (entity-update-tx :db-part1 :type1 attributes db-data data "invalid")
              (is false "Should throw"))
            (catch [:type :optimistic-locking-failure] {:keys [:v]}
              (is (= :db-version v))))
      ))
  )

(deftest entity-delete-tx-test
  (testing
    (let [db-data {:entity/version :db-version}
          tx (entity-delete-tx db-data 678 123)
          ]
      (is (=
            '([:optimistic-lock 678 123]
               [:db.fn/retractEntity 678]
               )
            tx))
      ))
  (testing
    "Invalid version"
    (let [db-data {:entity/version :db-version}]
      (try+ (do
              (entity-delete-tx db-data 678 "invalid")
              (is false "Should throw"))
            (catch [:type :optimistic-locking-failure] {:keys [:v]}
              (is (= :db-version v)))))
    )
  )
