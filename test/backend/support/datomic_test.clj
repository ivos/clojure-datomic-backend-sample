(ns backend.support.datomic-test
  (:require [backend.support.datomic :refer :all]
            [midje.sweet :refer :all]))

(facts
  "Prepare query params"
  (let [attributes [:with-value :empty-string :a-nil :number-zero :false-bool :number-non-zero :missing]
        data {:with-value "abc"
              :empty-string ""
              :a-nil nil
              :number-zero 0
              :false-bool false
              :number-non-zero 123
              :invalid "some-value"}
        expected {:with-value "abc"
                  :empty-string :nil
                  :a-nil :nil
                  :number-zero 0
                  :false-bool false
                  :number-non-zero 123
                  :invalid "some-value"
                  :missing :nil}]
    (prepare-query-params data attributes nil) => expected))
