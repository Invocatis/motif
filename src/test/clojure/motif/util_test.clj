(ns motif.util-test
  (:require [clojure.test :refer :all]
            [motif.util :refer :all]))

(defn yes [x] (= x true))
(defn no [x] (= x false))

(deftest conjunction-test
  (testing "Conjunctions"
    (are [f x y] (f (apply (conjunction x y) [1]))
      yes  pos? pos?
      yes  pos? neg?
      yes  neg? pos?
      no   neg? neg?)))

(deftest disjunction-test
  (testing "Disjunctions"
    (are [f x y] (f (apply (disjunction x y) [1]))
      yes  pos? pos?
      no   pos? neg?
      no   neg? pos?
      no   neg? neg?)))
