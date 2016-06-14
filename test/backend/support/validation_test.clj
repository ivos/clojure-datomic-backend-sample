(ns backend.support.validation-test
  (:require [slingshot.slingshot :refer [try+]]
            [backend.support.validation :refer :all]
            [midje.sweet :refer :all]))

(facts
  "verify-keys!"
  (facts
    "Ok"
    (let [attributes [:a :b :c :d]
          data {:a 1 :b 2 :d 4}]
      (verify-keys! attributes data) => nil
      ))
  (facts
    "Invalid attributes"
    (let [attributes [:a :b :c :d]
          data {:a 1 :b 2 :e 4 :f 5}]
      (try+ (do
              (verify-keys! attributes data)
              (fact "Should throw"
                    true => false))
        (catch [:type :backend.support.validation/validation-failure] {:keys [errors]}
          errors => {:e ["invalid.attribute"] :f ["invalid.attribute"]})
        ))
    )
  )
