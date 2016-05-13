(ns backend.entity-test
  (:require [clojure.test :refer :all]
            [backend.entity :refer :all]))

(deftest ns-keys-test
  (testing
    "Found is replaced."
    (is (= 
          {:my-entity/attr 1}
          (ns-keys [:my-entity/attr] {:attr 1}))))
  (testing
    "Not found is kept intact."
    (is (= 
          {:non-existing 1}
          (ns-keys [:my-entity/attr] {:non-existing 1}))))
  (testing
    "Multiple namespaces."
    (is (= 
          {:my-entity/attr-a 1 :other-entity/attr-b 2}
          (ns-keys [:my-entity/attr-a :other-entity/attr-b] {:attr-a 1 :attr-b 2}))))
  )
