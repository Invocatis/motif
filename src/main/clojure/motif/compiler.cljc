(ns motif.compiler
  (:require
   [clojure.walk :as walk]
   [motif.ancestory :refer [lineage youngest]]
   [motif.syntax :as syntax])
  (:refer-clojure :exclude [compile]))

(syntax/init!)

(defprotocol IMatchable
  (compile-matcher [_ context target]))

(defprotocol IStrictModifier
  (strictly [_ context target]))

(defprotocol IConjunctiveModifier
  (conjunctively [_ context target]))

(defprotocol IDisjunctiveModifier
  (disjunctively [_ context target]))

(defprotocol IMetaModifier
  (match-meta [_ context target]))

(defprotocol IGetterModifier
  (with-getter [_ context target]))

(defprotocol IUseModifier
  (use-compare [_ context target]))

(defprotocol IGuardModifier
  (guard [_ context target]))

(declare cpm*)

(extend-type clojure.lang.AFn
  IMatchable
  (compile-matcher [pattern context target]
    #[:apply pattern #[:access target (lineage context :element)]]))

(extend-type clojure.lang.APersistentSet
  IMatchable
  (compile-matcher [pattern context target]
    (into
     #[:or]
     (map
      (fn [p] (cpm* p context target))
      pattern)))

  IStrictModifier
  (strictly [pattern context target]
    (assoc (cpm* pattern context target) 0 :xor))

  IConjunctiveModifier
  (conjunctively [pattern context target]
    (cpm* pattern context target))

  IDisjunctiveModifier
  (disjunctively [pattern context target]
    (assoc (cpm* pattern context target) 0 :and)))

(extend-type java.util.regex.Pattern
  IMatchable
  (compile-matcher [pattern context target]
    #[:apply re-matches pattern #[:apply str #[:access target (lineage context :element)]]]))

(extend-type clojure.lang.APersistentVector
  IMatchable
  (compile-matcher [pattern context target]
    (into
     #[:and]
     (map
      (fn [p i] (cpm* p {:parent context :element i} target))
      pattern
      (range))))

  IStrictModifier
  (strictly [pattern context target]
    #[:and
      #[:apply = #[:apply count pattern] #[:apply count #[:access target (lineage context :element)]]]
      (cpm* pattern context target)])

  IDisjunctiveModifier
  (disjunctively [pattern context target]
    (cpm* pattern context target))

  IConjunctiveModifier
  (conjunctively [pattern context target]
    (assoc (cpm* pattern context target) 0 :or)))

(defn compile-seq
  [pattern context target index]
  (if (empty? pattern)
    true
    #[:or
      #[:apply = index #[:apply count #[:access target (lineage context :element)]]]
      #[:and
        (cpm* (first pattern) {:parent context :element index} target)
        #[:recur #[:apply compile-seq (rest pattern) context target (inc index)]]]]))

(extend-type clojure.lang.ISeq
  IMatchable
  (compile-matcher [pattern context target]
    (compile-seq pattern context target 0))

  IStrictModifier
  (strictly [pattern context target]
    #[:and
      #[:apply = #[:apply count pattern] #[:apply count #[:access target (lineage context :element)]]]
      (cpm* pattern context target)])

  IConjunctiveModifier
  (conjunctively [pattern context target]
    (assoc (cpm* pattern target context) 0 :or)))

(extend-type clojure.lang.APersistentMap
  IMatchable
  (compile-matcher [pattern context target]
    (into #[:and] (map (fn [[k v]] (cpm* v {:parent context :element k} target)) pattern)))

  IStrictModifier
  (strictly [pattern context target]
    #[:and
      #[:apply = #[:apply keys pattern] #[:apply keys target]]
      (cpm* pattern context target)])

  IConjunctiveModifier
  (conjunctively [pattern context target]
    (into #[:or] (rest (cpm* pattern context target))))

  IDisjunctiveModifier
  (disjunctively [pattern context target]
    (cpm* pattern context target))

  IGetterModifier
  (with-getter [_ {:keys [getter pattern] :as context} target]
    (cpm*
      (into {}
        (map
         (fn [[k v]] [#(getter % k) v])
         pattern))
      context
      target)))

(extend-type clojure.lang.Symbol
  IMatchable
  (compile-matcher [pattern context target]
    (if (= pattern '_)
      true
      #[:unify pattern target (lineage context :element)])))

(extend-type java.lang.Object
  IMatchable
  (compile-matcher [pattern context target]
    #[:apply = pattern #[:access target (lineage context :element)]])

  IGuardModifier
  (guard [pattern {:keys [guard] :as context} target]
    #[:and
      (cpm* guard context target)
      (cpm* pattern context target)]))

(extend-type clojure.lang.IObj
  IMetaModifier
  (match-meta [pattern {:keys [meta] :as context} target]
    #[:and
      (cpm* meta {:parent context :element clojure.core/meta} target)
      (cpm* pattern context target)])

  IUseModifier
  (use-compare [pattern {:keys [use] :as context} target]
    #[:apply use pattern #[:access target (lineage context :element)]]))

(extend-type nil
  IMatchable
  (compile-matcher [_ context target]
    #[:apply nil? #[:access target (lineage context :element)]]))

(defn star
  [pattern {:keys [*] :as context} target]
  (if (true? *)
    (recur pattern (assoc context :* 0) target)
    (let [pattern (nth (iterate #(repeat 10 %) pattern) (inc *))] ; TODO REMOVE
      (cpm* pattern context target))))

(def modifiers
  (atom
   [[:* star]
    [:meta match-meta]
    [:guard guard]
    [:use use-compare]
    [:= (fn [pattern context target] (use-compare pattern (assoc context :use =) target))]
    [:! strictly]
    [:| conjunctively]
    [:& disjunctively]
    [:getter with-getter]]))

(defn cpm*
  [pattern context target]
  (let [[tag modifier] (and (meta pattern)
                            (->> @modifiers
                                 (filter (fn [[k v]] (contains? (meta pattern) k)))
                                 first))
        meta-value (when tag (get (meta pattern) tag))
        pattern (if tag (with-meta pattern (dissoc (meta pattern) tag)) pattern)
        context context]
    (if modifier
      (apply modifier [pattern (assoc context tag meta-value) target])
      (compile-matcher pattern context target))))

(defn compile
  [pattern target]
  (cpm* pattern nil target))
