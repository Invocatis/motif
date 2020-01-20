(ns motif.core)

(declare compile-pattern)

(defn- and-pattern
  [p1 p2]
  (fn [target] (and (p1 target) (p2 target))))

(defn- strict?
  [pattern]
  (-> pattern meta :!))

(defn- compile-meta
  [pattern accessor]
  (and-pattern
    (compile-pattern (with-meta pattern (dissoc (meta pattern) :meta)) accessor)
    (compile-pattern (:meta (meta pattern)) (comp meta accessor))))

(defn- compile-arity
  [pattern accessor]
  (let [matcher (compile-pattern (with-meta pattern (dissoc (meta pattern) :*)))]
    (fn [target] (every? matcher (accessor target)))))

(defn- compile-use
  [pattern accessor]
  (fn [target] ((:use (meta pattern)) pattern (accessor target))))

(defn- compile-element
  [pattern accessor]
  (cond
    (fn? pattern) #(boolean (pattern (accessor %)))
    :else #(= (accessor %) pattern)))

(defn- compile-simple-map
  [pattern accessor]
  (reduce
    every-pred
    (map
      (fn [[k v]]
        (let [acc (if (ifn? k) k #(get % k))]
          (compile-pattern v
            (comp acc accessor))))
      pattern)))

(defn- compile-map
  [pattern accessor]
  (if (empty? pattern)
    (if (strict? pattern)
      (fn [target] (empty? (accessor target)))
      (fn [target] true))
    (if (strict? pattern)
      (and-pattern
        (compile-simple-map pattern accessor)
        (fn [target] (every? (partial contains? (accessor target)) (keys pattern))))
      (compile-simple-map pattern accessor))))

(defn- compile-simple-vector
  [pattern accessor]
  (let [subpatterns (map-indexed
                      (fn [i p] (compile-pattern p (comp #(nth % i) accessor)))
                      pattern)]
    (fn [target]
      (and
        (<= (count pattern) (count target))
        (every? #(% (accessor target)) subpatterns)))))

(defn- compile-vector
  [pattern accessor]
  (if (strict? pattern)
    (and-pattern
      (fn [target] (= (count pattern) (count (accessor target))))
      (compile-simple-vector pattern accessor))
    (compile-simple-vector pattern accessor)))

(defn- compile-seq
  [pattern accessor]
  (fn [value]
    (let [n (count (accessor value))]
      (every?
        boolean
        (map
          #(apply % [value])
          (take n
            (map-indexed
              (fn [i p]
                (compile-pattern p
                  (comp #(nth % i nil) accessor)))
              pattern)))))))

(defn- compile-set
  [pattern accessor]
  (let [subpatterns (map #(compile-pattern % accessor) pattern)]
    (if (strict? pattern)
      (fn [target]
        (every? identity (map (fn [sp] (sp target)) subpatterns)))
      (fn [target]
        (not (empty? (filter identity (map (fn [sp] (sp target)) subpatterns))))))))

(defn- compile-regex
  [pattern accessor]
  (fn [value]
    (boolean (re-matches pattern (-> value accessor str)))))

(def ^:private regex-type (type #""))

(defn- regex?
  [any]
  (= (type any) regex-type))

(defn- compile-pattern
  ([pattern]
   (compile-pattern pattern identity))
  ([pattern accessor]
   (cond
     (-> pattern meta :*)
     (compile-arity pattern accessor)

     (-> pattern meta :meta)
     (compile-meta pattern accessor)

     (-> pattern meta :=)
     (compile-use pattern accessor =)

     (-> pattern meta :use)
     (compile-use pattern accessor)

     (map? pattern)
     (compile-map pattern accessor)

     (set? pattern)
     (compile-set pattern accessor)

     (vector? pattern) (compile-vector pattern accessor)

     (seq? pattern) (compile-seq pattern accessor)

     (regex? pattern) (compile-regex pattern accessor)

     :else (compile-element pattern accessor))))

(defn matches?
  "Given a pattern, and an expression, recursively determines
  if the expression matches the pattern. Patterns are described
  as a DSL of clojure.core data structures as such:

  For f, a function, and x, any expression:

    (match f e) => (f e)

  For v and e, both vectors, each ordinal spot is checked:

    (match v e) => (and (= (count v) (count e)
                        (match v1 e1)
                        (match v2 e2) ...
                        (match vn en))

  For s, an explicit sequence of any length (or infinite),
    and e, a seqable collection of length n:

    (match s e) => (and (match s1 e1) (match s2 e2) ... (match sn en))

  For m, a map with keyset {k1,k2,...,kn}, and n, a map:

    (match m n) => (and (match (get m k1) (get n k1))
                        (match (get m k2) (get n k2)) ...
                        (match (get m kn) (get n kn))

  Furthermore, simple logical operations are accomplished by
  passing a vector of the desired operation, followed by a
  the patterns to be applied. As such, use:

    [:and p1 p2 ...]
    [:or p1 p2 ...]
    [:not p1]

  For any pattern not described above, equality is checked.

    (match 1 2) => (= 1 2)

  Given the expression passed matches the given pattern,
  true will be returned. Otherwise, false will be returned.
  "
  ([pattern]
   (compile-pattern pattern))
  ([pattern expr]
   (apply (compile-pattern pattern) [expr])))

(defmacro match
  "Takes a subject expression, and a set of clauses.
  Each clause should be of the form:

    pattern resultant

  For each clause, (match pattern expr) is performed. If it
  returns logical true, the clause is a match and the resultant
  is returned. A single default expression can follow the clauses
  and its value will be returned if no clause matches. If no
  default expression is provided, and no clause matches, nil will
  be returned"
  ([expr]
   nil)
  ([expr default]
   `~default)
  ([expr pattern result & statements]
   `(if (matches? ~pattern ~expr)
      ~result
      ~(cons `match (cons expr statements)))))
