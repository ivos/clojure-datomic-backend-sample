(ns backend.datomic-test
  (:require [clojure.test :refer :all]
            [backend.datomic :refer :all]))

(deftest prepare-query-params-test
  (testing
    (let [attributes [:with-value :empty-string :a-nil :number-zero :false-bool :number-non-zero :missing]
          data {:with-value "abc"
                :empty-string ""
                :a-nil nil
                :number-zero 0
                :false-bool false
                :number-non-zero 123
                :invalid "some-value"}]
      (is (= (prepare-query-params data attributes nil)
             {:with-value "abc"
              :empty-string :nil
              :a-nil :nil
              :number-zero 0
              :false-bool false
              :number-non-zero 123
              :invalid "some-value"
              :missing :nil}
             ))))
  )
