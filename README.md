# motif

Recursive, data driven, pattern matching for Clojure

## Releases and dependency information

Latest release: 0.1.0
```
[motif "0.1.0"]
```
```
<dependency>
  <groupId>motif</groupId>
  <artifactId>motif</artifactId>
  <version>0.1.0</version>
</dependency>
```
## Overview

Motif brings powerful, recursive, and fun pattern matching Clojure. Focusing on the power of the data and functions, not macros, Motif provides dynamic pattern matching that works magically well with all that Clojure has provided us.

Let's get started!

```clojure
(require '[motif.core :refer [matches?]])
```

## Literal patterns
Literals, when used in patterns, simple invoke an equality check on the given values.

```clojure
(matches? 1 1) ;=> true

(matches? 1 2) ;=> False
```
## Function patterns
Functions will be invoked against their given value. Currently, only predicate functions are supported (any -> boolean).

```clojure
(matches? pos? 1) ;=> true

(matches? string? 1) ;=> false
```

## Regex patterns
Simply enough, when a regex is matched against a value, the string of that value is used as the input for a standard regular expression matching. Thus, a string value or any other Clojure value may be used.

```clojure
(matches? #"\d*" "123") ;=> true

(matches? #"\d*" 123) ;=> true

(matches? #"\[(\d*\s)*\d*\]" [1 2 3]) ;=> true
```

## Vector patterns
Vector patterns expect to be matched against other seqables, and return false otherwise. Furthermore, vectors only match against same length values.

```clojure
(matches? [1 2] [1 2]) ;=> true

(matches? [pos? neg?] [1 -1]) ;=> true
```

## Seq patterns
Seq patterns work similarly to vector patterns, however, they will match on unequal length values. Lazy or infinite sequences may be used as well, however, beware of the potential for infinite loops!

```clojure
(matches? '(1 2 3) [1]) ;=> true

(matches? '(1) [1 2 3 4]) ;=> true

(matches? (repeat even?) [2 2 2 2]) ;=> true

(matches? (repeat odd?) (repeat 1)) ;=> infinite loop!
```
## Map patterns
Maps have act in very predictable ways, with some fun and interesting caveats. Let's analyze their predictability first.

In simple cases, for a given key-value pair in a map pattern, the value associated with the same key in a target map is is matched against the corresponding pattern value.

```clojure
(matches? {:key :pattern-value} {:key :target-value}) ;== (matches? :pattern-value :target-value)

(matches? {1 2} {1 2 3 4}) ;=> true

(matches? {1 2 3 4} {1 2}) ;=> false

(matches? {:key :value :key1 nil} {:key :value}) ;=> true
```

Furthermore, not all keys in the target need be present in the pattern (as seen in the previous example). The opposite is also true, given that the key is being matched against nil.

When accessing a maps value using a key, motif will apply the key as a function if at all possible (ifn?). In all other cases, the get function will be used instead.

This illuminates some fascinating possibilities for us; we can use function as pattern keys to get a more holistic look on our maps or even on other value types.

```clojure
(matches? {:key 1 #(count (keys %)) 1} {:key 1}) ;=> true

(matches? {pos? false neg? false} 0) ;=> true
```

## Set patterns

Sets work differently than all of our previous patterns. Given a set matching against an atomic target, motif checks that at least one of the set's patterns matches against the given target. Given a seqable target, motif checks that all values in the collection match the set.

```clojure
(matches? #{1 2 3} 1) ;=> true

(matches? #{1 2} [1 1 1 2 2]) ;=> true

(matches? #{1} [1 2]) ;=> false
```

Let's see a useful combination of our map and set patterns. Let's see how we could check if a map matches a pattern and additionally only contains a given set of keys.

```clojure
(matches? {:x 1 :y 2 keys #{:x :y}} {:x 1 :y 2}) ;=> true

(matches? {:x 1 :y 2 keys #{:x :y}} {:x 1 :y 2 :z 3}) ;=> false
```

## Logic patterns

Fantastically enough, we need not introduce any new magic in this section! Everything we need for simple logical disjunctions and conjunctions is already at our finger tips. In fact, we just went over them: maps serve us a disjunctions, and sets are our conjunctions.

Let's dive deeper: we saw that with maps, each key must satisfy its associated pattern. However, as we can use function as our keys, the disjunctive pattern simply falls out for us.

```clojure
(matches? {pos? false neg? false} 0) ;=> true

(matches? {even? true neg? true} -3) ;=> false
```

Great! Onto sets and conjunctions: sets require that the target matches one of its contained patterns. Sounds very much like an `or` clause.

```clojure
(matches? #{pos? neg?} -1) ;=> true

(matches? #{str? int?} 1) ;=> true
```

## That's it!

That's all you need to go on into the world. But before you go, let's look at some fun examples that may come in handy some day:

```clojure
(matches? (flatten (repeat (list odd? even?))) [1 2 3 4]) ;=> true

(matches? {first 1 last 4} [1 2 3 4]) ;=> true

(matches? {0 1 1 2 2 3 3 4} [1 2 3 4]) ;=> true

(matches? {(partial reduce max) 4} [1 2 3 4]) ;=> true

(matches? #{{inc 1} {dec 1}} 2) ;=> true

(matches? #{(repeat odd?) (repeat even?)} [1 1 1 1 1 1]) ;=> true
```
