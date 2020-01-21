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
    (is (matches? ^:! #{1 neg?} 1))
    (is (not (matches? ^:! #{1 pos?} 1)))))

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
    (is (matches? ^:! ^:* #{pos? odd?} [2 4 6 8]))
    (is (not (matches? ^:! ^:* #{pos? even?} [2 4 6 9])))))

(deftest match-macro
  (testing "Match Macro"
    (is
      (match "Hello"
        1 "Not this one"
        [1 2 3] "Or this one"
        #{1 2 3} "Not at all"
        #"Hello" true))))

(deftest readme-examples
  (testing "Readme Examples"
    (is (matches? 1 1))
    (is (not (matches? 1 2)))
    (is (matches? pos? 1))
    (is (matches? inc 1))
    (is (not (matches? (fn [x y]) 1)))
    (is (matches? #"\d*" "123"))
    (is (matches? #"\d*" "123"))
    (is (matches? #"\[(\d*\s)*\d*\]" [1 2 3]))
    (is (matches? [1 2] [1 2]))
    (is (not (matches? [1 2] [1])))
    (is (matches? [1 2] [1 2 3]))
    (is (matches? [pos? neg?] [1 -1]))
    (is (matches? [\a \b] "ab"))
    (is (matches? '(1 2 3) [1]))
    (is (matches? '(1) [1 2 3 4]))
    (is (matches? (repeat even?) [2 2 2 2]))
    (is (matches? {1 2} {1 2 3 4}))
    (is (not (matches? {1 2 3 4} {1 2})))
    (is (matches? {:key1 :value :key2 nil} {:key1 :value}))
    (is (matches? {:key1 :value :key2 nil} {:key1 :value :key2 nil}))
    (is (matches? {:key 1 #(count (keys %)) 1} {:key 1}))
    (is (matches? {pos? false neg? false} 0))
    (is (matches? #{1 2 3} 1))
    (is (matches? #{even? odd?} 0))
    (is (not (matches? #{pos? neg?} 0)))
    (is (not (matches? {identity 1} {identity 1})))

    (is (matches? ^:= {identity 1} {identity 1}))
    (is (not (matches? {:x 1 :y 2} {:x 3 :y 4})))

    (is (matches? ^{:use #(= (set (keys %1)) (set (keys %2)))} {:x 1 :y 2} {:x 3 :y 4}))

    (matches? ^{:getter get} {pos? neg? neg? pos?} {pos? -2 neg? 2})

    (is (not (matches? #{1 2} [1 2])))

    (is (matches? ^:* #{1 2} [1 2]))
    (matches? ^{:* 1} (fn [n] (integer? n)) [[1 2] [3 4] [5 6]])
    (is (matches? {:y 1} ^{:x 1} {:y 1}))

    (is (not (matches? ^{:meta {:x 2}} {:y 1} ^{:x 1} {:y 1})))

    (is (matches? ^{:meta {:x ^:* #{1 2}}} {:y 1} ^{:x [1 2]} {:y 1}))
    (is (matches? {:x 1} {:x 1 :y 2}))

    (is (not (matches? ^:! {:x 1} {:x 1 :y 2})))

    (is (matches? ^:! {:x 1 :y 2} {:x 1 :y 2}))

    (is (matches? #{1 pos?} 1))

    (is (not (matches? ^:! #{1 pos?} 1)))

    (is (matches? [1 2 3] [1 2 3 4]))

    (is (not (matches? ^:! [1 2 3] [1 2 3 4])))

    (matches? ^:| {:x 1 :y 2} {:x 1 :y 3})

    (matches? ^:& #{pos? even?} -2)

    (is (not (matches? {:x 1 nil? true} {:x 1})))

    (is (matches? {:x 1} {:x 1 nil? true}))

    (is (not (matches? #(throw (Exception. "Some Exception")) nil)))

    (is (matches? (interleave (repeat odd?) (repeat even?)) [1 2 3 4]))

    (is (matches? {first 1 last 4} [1 2 3 4]))

    (is (matches? {0 1 1 2 2 3 3 4} [1 2 3 4]))

    (is (matches? {some? true inc 1} 0))

    (is (matches? {(partial reduce max) 4} [1 2 3 4]))

    (is (matches? #{{inc 1} {dec 1}} 2))

    (is (matches? (complement #{1 2 3}) 4))

    (is (matches? (repeat odd?) [1 1 1 1 1 1]))

    (is (matches? {(juxt inc dec even?) [2 0 false]} 1))

    (is (matches? ^{:getter (fn [target key] (inc key))} {0 1 1 2 2 3} {}))
    (is (matches? _ 1))
    (is (matches? _ nil))
    (is (matches? {:x _} {:x {:y 1}}))))
