(ns backend.datomic-test
  (:require [clojure.test :refer :all]
            [backend.datomic :refer :all]))

(deftest prepare-query-params-test
  (testing
    (let [attributes [:with-value :empty-string :a-nil :number-zero :false-bool :number-non-zero :keyword :missing]
          data {:with-value "abc"
                :empty-string ""
                :a-nil nil
                :number-zero 0
                :false-bool false
                :number-non-zero 123
                :keyword :keyword-value
                :invalid "some-value"}]
      (is (= (prepare-query-params data attributes)
             {:with-value "abc"
              :empty-string :nil
              :a-nil :nil
              :number-zero 0
              :false-bool false
              :number-non-zero 123
              :keyword :keyword-value
              :missing :nil}
             ))))
  )
