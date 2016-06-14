(ns backend.support.entity-test
  (:require [slingshot.slingshot :refer [try+]]
            [midje.sweet :refer :all]
            [backend.support.entity :refer :all]))

(facts
  "empty-strings-to-nils"
  (let [data {:with-value "abc"
              :empty-string ""
              :number-zero 0
              :false-bool false
              :number-non-zero 123
              :keyword :keyword-value}]
    (empty-strings-to-nils data)
      => {:with-value "abc"
       :empty-string nil
       :number-zero 0
       :false-bool false
       :number-non-zero 123
       :keyword :keyword-value})
  )

(facts
  "remove-nil-values"
  (let [data {:with-value "abc"
              :nil-value1 nil
              :number-zero 0
              :false-bool false
              :nil-value2 nil
              :number-non-zero 123
              :keyword :keyword-value}]
    (remove-nil-values data)
    => {:with-value "abc"
        :number-zero 0
        :false-bool false
        :number-non-zero 123
        :keyword :keyword-value})
  )

(facts
  "ensure-all-attributes"
  (let [data {:with-value "abc"
              :nil-value1 nil
              :false-bool false
              :number-non-zero 123
              :other "other-value"
              :keyword :keyword-value}
        attributes [:with-value :nil-value1 :number-non-zero :missing1 :missing2 :keyword]
        ]
    (ensure-all-attributes data attributes)
    => {:with-value "abc"
        :nil-value1 nil
        :false-bool false
        :number-non-zero 123
        :other "other-value"
        :keyword :keyword-value
        :missing1 nil
        :missing2 nil})
  )

(facts
  "ns-value"
  (fact
    "Ok"
    (let [data {:a 1 :an-attr "a-value"}]
      (ns-value data :an-attr :a-ns) => {:a 1 :an-attr :a-ns/a-value}))
  (fact
    "Attribute value is nil"
    (let [data {:a 1 :an-attr nil}]
      (ns-value data :an-attr :a-ns) => {:a 1 :an-attr nil}))
  (fact
    "Attribute is not present"
    (let [data {:a 1}]
      (ns-value data :an-attr :a-ns) => {:a 1}))
  )

(facts
  "ns-keys"
  (fact
    "Found is replaced"
    (ns-keys {:attr 1} [:my-entity/attr]) => {:my-entity/attr 1})
  (fact
    "Not found is kept intact"
    (ns-keys {:non-existing 1} [:my-entity/attr]) => {:non-existing 1})
  (fact
    "Multiple namespaces"
    (ns-keys {:attr-a 1 :attr-b 2} [:my-entity/attr-a :other-entity/attr-b])
    => {:my-entity/attr-a 1 :other-entity/attr-b 2})
  (fact
    "Nil value"
    (ns-keys {:attr nil} [:my-entity/attr]) => {:my-entity/attr nil})
  )

(facts
  "entity-create-tx"
  (let [attributes [:add1 :add2 :keep-nil3]
        data {:eid :v-eid
              :add1 :v-add1
              :add2 :v-add2
              :other :v-other}
        ]
    (entity-create-tx :db-part1 :type1 attributes data)
    => '([:db/add :v-eid :add1 :v-add1]
          [:db/add :v-eid :add2 :v-add2]
          [:db/add :v-eid :entity/version 1]
          [:db/add :v-eid :entity/type :type1])
    )
  )

(facts
  "entity-update-tx"
  (fact
    "Ok"
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
          ]
      (entity-update-tx :db-part1 :type1 attributes db-data data 123)
      => '([:optimistic-lock :v-eid 123]
            [:db/add :v-eid :entity/version 124]
            [:db/add :v-eid :modify1 :v-modify1]
            [:db/add :v-eid :modify2 :v-modify2]
            [:db/retract :v-eid :retract3 :db-retract3]
            [:db/add :v-eid :add4 :v-add4]
            )
      ))
  (facts
    "Invalid version"
    (let [attributes [:a]
          db-data {:a :db-a
                   :entity/version :db-version}
          data {:eid :v-eid
                :a :v-a}
          ]
      (try+ (do
              (entity-update-tx :db-part1 :type1 attributes db-data data "invalid")
              (fact "Should throw"
                    true => false))
            (catch [:type :optimistic-locking-failure] {:keys [:v]}
              v => :db-version))
      ))
  )

(facts
  "entity-delete-tx"
  (facts
    "Ok"
    (let [db-data {:entity/version :db-version}
          tx (entity-delete-tx db-data 678 123)
          ]
      tx => '([:optimistic-lock 678 123]
               [:db.fn/retractEntity 678]
               )
      ))
  (facts
    "Invalid version"
    (let [db-data {:entity/version :db-version}]
      (try+ (do
              (entity-delete-tx db-data 678 "invalid")
              (fact "Should throw"
                    true => false))
              (catch [:type :optimistic-locking-failure] {:keys [:v]}
                v => :db-version))
      ))
  )
