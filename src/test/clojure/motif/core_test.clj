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

(deftest test-strict-map
  (testing "Strict Maps"
    (is (matches? ^:! {:x 1 :y 2} {:x 1 :y 2}))
    (is (not (matches? ^:! {:x 1} {:x 1 :y 2})))))

(deftest test-vector
  (testing "Vectors"
    (is (matches? [pos? pos? neg?] [1 1 -1]))
    (is (matches? [1 2] [1 2 3]))
    (is (not (matches? [1] [])))))

(deftest test-set
  (testing "Sets"
    (is (matches? #{1 2} 1))
    (is (not (matches? #{1 2} 3)))))

(deftest test-strict-sets
  (testing "Strict Sets"
    (is (matches? ^:! #{pos? 1} 1))
    (is (not (matches? ^:! #{1 2} 1)))))

(deftest test-seq
  (testing "Seqs"
    (is (matches? '(1) [1 1 1]))
    (is (matches? (repeat nil?) [nil nil nil nil nil]))
    (is (matches? (flatten (repeat (list pos? neg?))) [1 -1 1 -1 1 -1 1]))))

(deftest test-regex
  (testing "Regexes"
    (is (matches? #"\d*" "123"))
    (is (matches? #"\[(\d\s)*\d?\]" [1 2 3 4]))))

(deftest test-star-modifier
  (testing "Star Modifier"
    (is (matches? ^:* #{1 2 3} [1 2 3]))
    (is (matches? ^:* (fn [x] (pos? x)) [1 2 3]))
    (is (matches? ^:* {:x 1 :y 2} [{:x 1 :y 2 :z 3} {:x 1 :y 2}]))
    (is (not (matches? ^:* #{1 2} [1 4])))))

(deftest test-equality-modifier
  (testing "Equality Modifier"
    (is (matches? ^:= {keys 1 println 2} {keys 1 println 2}))
    (is (not (matches? ^:= {:x 1 :y 2} {:x 1 :y 2 :z 3})))))

(deftest test-use-modifier
  (testing "Use Modifier"
    (is (matches? ^{:use not=} {:x 1} {:y 2}))))

(deftest test-meta-modifier
  (testing "Meta Modifier"
    (is (matches? ^{:meta {:x #{1 2 3}}} {:z 2} ^{:x 1} {:z 2}))
    (is (not (matches? ^{:meta {:x ^:! {:x 1 :y 2}}} {:z 2} ^{:x {:x 1 :y 2 :z 3}} {:z 2})))))

(deftest compound-test
  (testing "Compound Test"
    (is (matches? ^:! ^:* #{pos? even?} [2 4 6 8]))
    (is (not (matches? ^:! ^:* #{pos? even?} [2 4 6 9])))))

(deftest match-macro
  (testing "Match Macro"
    (is
      (match "Hello"
        1 "Not this one"
        [1 2 3] "Or this one"
        #{1 2 3} "Not at all"
        #"Hello" true))))
