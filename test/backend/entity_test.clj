(ns backend.entity-test
  (:require [clojure.test :refer :all]
            [slingshot.slingshot :refer [try+]]
            [backend.entity :refer :all]))

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
  )

(deftest entity-create-tx-test
  (testing
    (let [attributes [:add1 :add2 :keep-nil3]
          data {:id :v-id
                :add1 :v-add1
                :add2 :v-add2
                :other :v-other}
          tx (entity-create-tx :db-part1 :type1 attributes data)
          ]
      (is (=
            '([:db/add :v-id :add1 :v-add1]
               [:db/add :v-id :add2 :v-add2]
               [:db/add :v-id :entity/version 1]
               [:db/add :v-id :entity/type :type1])
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
                :id :v-id
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
            '([:optimistic-lock :v-id 123]
               [:db/add :v-id :entity/version 124]
               [:db/add :v-id :modify1 :v-modify1]
               [:db/add :v-id :modify2 :v-modify2]
               [:db/retract :v-id :retract3 :db-retract3]
               [:db/add :v-id :add4 :v-add4]
               )
            tx))
      ))
  (testing
    "Invalid version"
    (let [attributes [:a]
          db-data {:a :db-a
                   :entity/version :db-version}
          data {:id :v-id
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
