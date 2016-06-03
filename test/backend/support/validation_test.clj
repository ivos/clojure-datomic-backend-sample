(ns backend.support.validation-test
  (:require [clojure.test :refer :all]
            [slingshot.slingshot :refer [try+]]
            [backend.support.validation :refer :all]))

(deftest verify-keys!-test
  (testing
    "Ok."
    (let [attributes [:a :b :c :d]
          data {:a 1 :b 2 :d 4}]
      (is (=
            nil
            (verify-keys! attributes data)))))
  (testing
    "Invalid attributes"
    (let [attributes [:a :b :c :d]
          data {:a 1 :b 2 :e 4 :f 5}]
      (try+ (do
              (verify-keys! attributes data)
              (is false "Should throw"))
        (catch [:type :backend.support.validation/validation-failure] {:keys [errors]}
          (is (= errors
                 {:e ["invalid.attribute"] :f ["invalid.attribute"]})))
        ))
    )
  )
