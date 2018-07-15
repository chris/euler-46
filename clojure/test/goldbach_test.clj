(ns goldbach-test
  (:require [clojure.test :refer :all]
            [goldbach :refer :all]))

(deftest test-check-possible-solution
  (testing "valid combinations"
    (is (true? (check-possible-solution 9 7 1)))
    (is (true? (check-possible-solution 15 7 2)))
    (is (true? (check-possible-solution 15 13 1)))
    (is (true? (check-possible-solution 27 19 2)))
    (is (true? (check-possible-solution 33 31 1))))
  (testing "invalid combos"
    (is (false? (check-possible-solution 15 11 1)))
    (is (false? (check-possible-solution 33 19 3)))
  (testing "pre-condition that prime can't be >= to the odd composite"
    (is (thrown? AssertionError (check-possible-solution 15 15 1)))
    (is (thrown? AssertionError (check-possible-solution 15 19 1))))))

(deftest test-primes-below
  (testing "it works"
    (is (= [2 3 5 7] (primes-below 10)))))

(deftest test-solution-for-odd-comp-and-prime
  (testing "failure cases"
    (is (nil? (solution-for-odd-comp-and-prime 15 5))))
  (testing "solution cases"
    (is (= [7 2] (solution-for-odd-comp-and-prime 15 7)))
    (is (= [13 1] (solution-for-odd-comp-and-prime 15 13)))))

(deftest test-solution-for-odd-comp
  (testing "success cases"
    (is (= [13 1] (solution-for-odd-comp 15)))
    (is (= [19 2] (solution-for-odd-comp 27)))))

(deftest test-odd-composite-numbers
  (testing "returns odd numbers, and excludes primes"
    (is (= [9 15 21 25 27 33] (take 6 (odd-composite-numbers))))))

(deftest test-not-prime
  (testing "not-prime?"
    (is (true? (not-prime? 8)))
    (is (true? (not-prime? 35)))
    (is (false? (not-prime? 7)))
    (is (false? (not-prime? 31)))))

(deftest test-next-odd-composite
  (testing "it works"
    (is (= 15 (next-odd-composite 9)))
    (is (= 33 (next-odd-composite 27)))))
