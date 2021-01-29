(ns motif.core
  (:require
   [motif.interpreter :refer [interpret]]))

(defprotocol IMatchable
  (compile-matcher [pattern target context]))

(defprotocol IContinuable
  (continue [pattern target]))

(defprotocol IStrictModifier
  (strictly [pattern target context]))

(defprotocol IConjunctiveModifier
  (conjunctively [pattern target context]))

(defprotocol IDisjunctiveModifier
  (disjunctively [pattern target context]))

(defprotocol IMetaModifier
  (match-meta [pattern target context]))

(defprotocol IGetterModifier
  (with-getter [pattern target context]))

(defprotocol IUseModifier
  (use-compare [pattern target context]))

(declare cpm)

(extend-type clojure.lang.AFn
  IMatchable
  (compile-matcher [pattern target context]
    [:apply pattern [:access target context]]))

(extend-type clojure.lang.APersistentSet
  IMatchable
  (compile-matcher [pattern target context]
    (into
     [:or]
     (map
      (fn [p] (cpm p target context))
      pattern))))

(extend-type clojure.lang.APersistentSet
  IStrictModifier
  (strictly [pattern target context]
    (into
     [:xor]
     (map
      (fn [p] (cpm p target context))
      pattern))))

(extend-type clojure.lang.APersistentSet
  IConjunctiveModifier
  (conjunctively [pattern target context]
    (cpm pattern target context)))

(extend-type clojure.lang.APersistentSet
  IDisjunctiveModifier
  (disjunctively [pattern target context]
    (assoc (cpm pattern target context) 0 :and)))

(extend-type java.util.regex.Pattern
  IMatchable
  (compile-matcher [pattern target context]
    [:apply re-matches pattern [:apply str target]]))

(extend-type clojure.lang.APersistentVector
  IMatchable
  (compile-matcher [pattern target context]
    (into
     [:and]
     (map
      (fn [p i] (cpm p target (update context :accessor conj i)))
      pattern
      (range)))))

(extend-type clojure.lang.APersistentVector
  IStrictModifier
  (strictly [pattern target context]
    [:and
     [:apply = [:apply count pattern] [:apply count [:access target context]]]
     (cpm pattern target context)]))

(extend-type clojure.lang.ASeq
  IConjunctiveModifier
  (conjunctively [pattern target context]
    (into [:or] (rest (cpm pattern target context)))))

(extend-type clojure.lang.ASeq
  IMatchable
  (compile-matcher [pattern target context]
    [:and
     [:apply cpm (first pattern) [:apply first [:access target context]] (assoc context :accessor [])]
     [:if [:apply empty? [:access target context]]
      true
      [:recur [:apply cpm [:constant (rest pattern)] [:apply rest [:constant target]] context]]]]))

(extend-type clojure.lang.ASeq
  IStrictModifier
  (strictly [pattern target context]
    [:and
     [:apply = [:apply count pattern] [:apply count [:access target context]]]
     (cpm pattern target context)]))

(extend-type clojure.lang.ASeq
  IConjunctiveModifier
  (conjunctively [pattern target context]
    (into [:or] (rest (cpm pattern target context)))))

(extend-type clojure.lang.APersistentMap
  IMatchable
  (compile-matcher [pattern target context]
    (into [:and] (map #(cpm % target context) pattern))))

(extend-type clojure.lang.APersistentMap
  IStrictModifier
  (strictly [pattern target context]
    [:and
     [:apply = [:apply keys pattern] [:apply keys target]]
     (cpm pattern target context)]))

(extend-type clojure.lang.APersistentMap
  IConjunctiveModifier
  (conjunctively [pattern target context]
    (into [:or] (rest (cpm pattern target context)))))

(extend-type clojure.lang.APersistentMap
  IDisjunctiveModifier
  (disjunctively [pattern target context]
    (cpm pattern target context)))

(extend-type clojure.lang.AMapEntry
  IMatchable
  (compile-matcher [pattern target context]
    (cpm (val pattern) target
      (update context :accessor conj (key pattern)))))

(extend-type clojure.lang.Symbol
  IMatchable
  (compile-matcher [pattern target context]
    (if (= pattern '_)
      true
      [:unify pattern target context])))

(extend-type java.lang.Object
  IMatchable
  (compile-matcher [pattern target context]
    [:apply = pattern [:access target context]]))

(extend-type clojure.lang.IObj
  IMetaModifier
  (match-meta [pattern target {meta-pattern :meta :as context}]
    [:and
     (cpm meta-pattern target (update context :accessor conj meta))
     (cpm pattern target context)]))

(extend-type clojure.lang.IObj
  IUseModifier
  (use-compare [pattern target {:keys [use]}]
    [:apply use pattern target]))

(extend-type nil
  IMatchable
  (compile-matcher [pattern target context]
    [:apply nil? [:access target context]]))

(declare matches?)

(defn star
  [pattern target {:keys [*] :as context}]
  (if (and (number? *) (> * 0))
    (every? #(star pattern % (update context :* dec)) target)
    [:apply every? #(matches? pattern %) [:access target context]]))

(def modifiers
  [[:* star]
   [:meta match-meta]
   [:use use-compare]
   [:= (fn [pattern target context] (use-compare pattern target (assoc context :use =)))]
   [:! strictly]
   [:| conjunctively]
   [:& disjunctively]
   [:getter with-getter]])

(def _ (constantly true))

(defn cpm
  [pattern target & [context]]
  (let [[tag modifier] (and (meta pattern)
                            (->> modifiers
                                 (filter (fn [[k v]] (contains? (meta pattern) k)))
                                 first))
        meta-value (when tag (get (meta pattern) tag))
        pattern (if tag (with-meta pattern (dissoc (meta pattern) tag)) pattern)
        context (or context {:accessor []})]
    (if modifier
      (apply modifier [pattern target (assoc context tag meta-value)])
      (apply compile-matcher [pattern target context]))))

(defn matches?
  [pattern target]
  (interpret (cpm pattern target) {} nil))
