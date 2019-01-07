(ns motif.core-test
  (:require [clojure.test :refer :all]
            [motif.core :refer :all]))

(defn yes [x] (= x true))
(defn no [x] (= x false))

(deftest equality-attribute
  (testing "Simple attributes"
    (is (matches? 1 1))
    (is (not (matches? 1 2)))))

(deftest fn-attribute
  (testing "Function attributes"
    (is (matches? {:test pos?} {:test 1}))
    (is (not (matches? {:test pos?} {:test -1})))))

(deftest test-map
  (testing "Maps"
    (is (matches? {:x 1} {:x 1 :y 2}))
    (is (not (matches? {:x 1} {:y 2})))))

(deftest test-vector
  (testing "Vectors"
    (is (matches? [pos? pos? neg?] [1 1 -1]))
    (is (not (matches? [] [nil])))))

(deftest test-set
  (testing "Sets"
    (is (matches? #{1 2} 1))
    (is (matches? #{1 2} [1 2]))
    (is (not (matches? #{1 2} 3)))
    (is (not (matches? #{1 2} [1 2 3])))))

(deftest test-seq
  (testing "Seqs"
    (is (matches? '(1) [1 1 1]))
    (is (matches? (repeat nil?) [nil nil nil nil nil]))
    (is (matches? (flatten (repeat (list pos? neg?))) [1 -1 1 -1 1 -1 1]))))

(deftest test-regex
  (testing "Regexes"
    (is (matches? #"\d*" "123"))
    (is (matches? #"\[(\d\s)*\d?\]" [1 2 3 4]))))

(deftest match-macro
  (testing "Match Macro"
    (is
      (match "Hello"
        1 "Not this one"
        [1 2 3] "Or this one"
        #{1 2 3} "Not at all"
        #"Hello" true))))
