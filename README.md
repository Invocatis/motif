# motif

Recursive, data driven, pattern matching for Clojure

## Releases and dependency information

```
[motif "1.0.0"]
```
```
<dependency>
  <groupId>motif</groupId>
  <artifactId>motif</artifactId>
  <version>1.0.0</version>
</dependency>
```
## Overview

Motif brings expressive pattern matching to Clojure. Motif focuses on using the power of our core Clojure data structures, giving each its own meaning and possibilities.

Let's get started!

```clojure
(require '[motif.core :refer [matches?] :as motif])
```

## Patterns

### Value Patterns
Simple value patterns are simply compared via equality.

```clojure
(matches? 1 1) ;=> true

(matches? 1 2) ;=> false
```
### Function patterns
Functions as patterns are invoked on the given target. Monadic predicates are only supported.

```clojure
(matches? pos? 1) ;=> true

(matches? string? 1) ;=> false

(matches? inc 1) ;=> true

(matches? (fn [x y]) 1) ;=> ArityException!
```

### Regex patterns
Regex patterns are compared to the string representations of their targets as with clojure.core/re-matches.

```clojure
(matches? #"\d*" "123") ;=> true

(matches? #"\d*" 123) ;=> true

(matches? #"\[(\d*\s)*\d*\]" [1 2 3]) ;=> true
```

### Vector patterns
Vectors compare each element oridnally, that is the first element of the pattern is compared to the first of the target, the second to the second, and so on. Vectors require their targets to be at least as long as their patterns, but the targets can be longer than the patterns.

```clojure
(matches? [1 2] [1 2]) ;=> true

(matches? [1 2] [1]) ;=> false

(matches? [1 2] [1 2 3]) ;=> true

(matches? [pos? neg?] [1 -1]) ;=> true

(matches? [\a \b] "ab") ;=> true
```

### Seq patterns
Seq patterns work similarly to vector patterns, however, they can be longer or shorter than their targets. This allows for infinite length sequences to be used as patterns, however, beware of infinite loops!

```clojure
(matches? '(1 2 3) [1]) ;=> true

(matches? '(1) [1 2 3 4]) ;=> true

(matches? (repeat even?) [2 2 2 2]) ;=> true

(matches? (repeat odd?) (repeat 1)) ;=> infinite loop!
```
### Map patterns
Maps compare corresponding key values with `matches?` Maps a conjunctive, and thus all keys much match positively. Extra keys in the target map are acceptable, and ignored by the pattern. Keys are not required to be present neither, though if they are not they're corresponding patterns must match against `nil`.

```clojure
(matches? {:key :pattern-value} {:key :target-value}) ;== (matches? :pattern-value :target-value)

(matches? {1 2} {1 2 3 4}) ;=> true

(matches? {1 2 3 4} {1 2}) ;=> false

(matches? {:key1 :value :key2 nil} {:key1 :value}) ;=> true

(matches? {:key1 :value :key2 nil} {:key1 :value :key2 nil}) ;=> true
```

If a key in the pattern is an IFn, it is instead applied to the target as apposed to gotten with `get`; this opens up another dimension of expressiveness using maps.

```clojure
(matches? {:key 1 #(count (keys %)) 1} {:key 1}) ;=> true

(matches? {pos? false neg? false} 0) ;=> true
```

### Set patterns

Sets are our disjunctive patterns; they need only one of their elements match against the target.

```clojure
(matches? #{1 2 3} 1) ;=> true

(matches? #{even? odd?} 0) ;=> true

(matches? #{pos? neg?} 0) ;=> false
```

## Modifiers

Modifiers extend and specify how each of patterns can work. Modifiers are attached to their patterns as metadata values.

### Equality Modifier

The Equality modifier directs motif to compare values using the `=` function, rather than `matches?`. We can use this when motif's special interpretations might get in the way.

```clojure
(matches? {println 1} {println 1}) ;=> false

(matches? ^:= {println 1} {println 1}) ;=> true
```

### Use Modifier

The Use modifier explicitly directs motif to use a different function than `matches?`. The Equality modifier is a special case of the use modifier.

```clojure
(matches? {:x 1 :y 2} {:x 3 :y 4}) ;=> false

(matches? ^{:use #(= (set (keys %1)) (set (keys %2)))} {:x 1 :y 2} {:x 3 :y 4}) ;=> true
```

### Star Modifier

The Star modifier maps the pattern over the target, requiring all elements to match.

```clojure
(matches? #{1 2} [1 2]) ;=> false

(matches? ^:* #{1 2} [1 2]) ;=> true
```

### Meta Modifier

Since modifiers take up the information space of the structures metadata, the Meta Modifier adds another area for metadata to be matched. That is, the metadata of the target is matched against the values in the Meta Modifier. The values are matched in the same way as in `matches?`, so all patterns discussed work within the modifier.

```clojure
(matches? {:y 1} ^{:x 1} {:y 1}) ;=> true

(matches? ^{:meta {:x 2}} {:y 1} ^{:x 1} {:y 1}) ;=> false

(matches? ^{:meta {:x ^:* #{1 2}}} {:y 1} ^{:x 1} {:y 1}) ;=> true
```

### Strict Modifier

The Strict modifier is interpreted different for each pattern type, though, it in general reduces some of the laxness that our patterns might have.

#### Strict Maps
Strict maps require equality in keys.

```clojure
(matches? {:x 1} {:x 1 :y 2}) ;=> true

(matches? ^:! {:x 1} {:x 1 :y 2}) ;=> false

(matches? ^:! {:x 1 :y 2} {:x 1 :y 2}) ;=> true
```

#### Strict Sets
Strict sets are conjunctive, rather than disjunctive. That is, all elements must match the target, not just one. Or rather, it means they serve as logical `and` vs logical `or`.

```clojure
(matches? #{1 2} 1) ;=> true

(matches? ^:! #{1 2} 1) ;=> false

(matches? ^:! #{pos? even?} [2 4 6 8]) ;=> true
```

#### Strict Vectors
Strict Vectors require the length of their targets be the same as their own length.

```clojure
(matches? [1 2 3] [1 2 3 4]) ;=> true

(matches? ^:! [1 2 3] [1 2 3 4]) ;=> false
```

## Some things to note

### Non-symmetry
matches? is not symmetric! That is (matches? a b) does not imply (matches? b a). This is due to patterns being treated differently that their targets. Consider the following:
```clojure
(matches {:x 1 nil? false} {:x 1}) ;=> true

(matches {:x 1} {:x 1 nil? false}) ;=> false
```

This dissymmetry is due to how various structures are interpreted when they are used as patterns.

### Exceptions are failures

Any exceptions thrown my the patterns, or motif itself, are treated as general failures, and simply cause `matches?` to return `false`

```clojure
(matches? #(throw (Exception. "Some Exception")) nil) ;=> Exception Some Exception
```

## That's it!

That's all you need to go out into the world. But before you go, let's look at some fun examples that might help illuminate some of the possibilities:

```clojure
(matches? (interleave (repeat odd?) (repeat even?)) [1 2 3 4]) ;=> true

(matches? {first 1 last 4} [1 2 3 4]) ;=> true

(matches? {0 1 1 2 2 3 3 4} [1 2 3 4]) ;=> true

(matches? {some? true inc 1} 0) ;=> true

(matches? {(partial reduce max) 4} [1 2 3 4]) ;=> true

(matches? #{{inc 1} {dec 1}} 2) ;=> true

(matches? (complement #{1 2 3}) 4) ;=> true

(matches? (repeat odd?) [1 1 1 1 1 1]) ;=> true

(matches? {(juxt inc dec even?) [2 0 false]} 1) ;=> true
```
