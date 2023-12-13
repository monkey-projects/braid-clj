(ns monkey.braid.test.utils-test
  (:require [clojure.test :refer [deftest testing is]]
            [monkey.braid.utils :as sut]))

(deftest ->transit
  (testing "encodes to transit string"
    (is (= "[\"^ \",\"~:key\",\"value\"]"
           (sut/->transit {:key "value"})))))
