(ns core-test
  (:require [cljs.test :refer-macros [deftest is testing]]))

(deftest noop-test
  (testing "nothing"
    (is (= 1 1))))
