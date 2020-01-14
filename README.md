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

Motif brings powerful, recursive, and fun pattern matching Clojure. Focusing on the power of data structures and functions, not macros, Motif provides dynamic powers matching that works magically well with all that Clojure has provided us.

Let's get started!

```clojure
(require '[motif.core :refer [matches?]])
```

## Literal patterns
Literals, when used in patterns, simply invoke an equality check on the given target.

```clojure
(matches? 1 1) ;=> true

(matches? 1 2) ;=> False
```
## Function patterns
Functions as patterns are invoked on the given target. Monadic functions are only supported.

```clojure
(matches? pos? 1) ;=> true

(matches? string? 1) ;=> false

(matches? inc 1) ;=> true
```

## Regex patterns
Regex patterns are compared to the string representations of their targets.

```clojure
(matches? #"\d*" "123") ;=> true

(matches? #"\d*" 123) ;=> true

(matches? #"\[(\d*\s)*\d*\]" [1 2 3]) ;=> true
```

## Vector patterns
Each element in a vector pattern is matched against the corresponding value in the given target. If a non-seqable target is given, or a seqable with differing length, then false is returned. Strings, when matched against vectors, are considered seqable.

```clojure
(matches? [1 2] [1 2]) ;=> true

(matches? [pos? neg?] [1 -1]) ;=> true

(matches? [\a \b] "ab") ;=> true
```

## Seq patterns
Seq patterns work similarly to vector patterns, however, they do not require their targets be of equal length. This allows for infinite length sequences to be used as patterns, however, beware of infinite loops!

```clojure
(matches? '(1 2 3) [1]) ;=> true

(matches? '(1) [1 2 3 4]) ;=> true

(matches? (repeat even?) [2 2 2 2]) ;=> true

(matches? (repeat odd?) (repeat 1)) ;=> infinite loop!
```
## Map patterns
Maps act in very predictable ways, with some fun and interesting caveats. Let's analyze their predictability first.

In 

cases, for a given key-value pair in a map pattern, the value associated with the same key in a target map is is matched against the corresponding pattern value.

```clojure
(matches? {:key :pattern-value} {:key :target-value}) ;== (matches? :pattern-value :target-value)

(matches? {1 2} {1 2 3 4}) ;=> true

(matches? {1 2 3 4} {1 2}) ;=> false

(matches? {:key1 :value :key2 nil} {:key1 :value}) ;=> true

(matches? {:key1 :value :key2 nil} {:key1 :value :key2 nil}) ;=> true
```

Furthermore, not all keys in the target need be present in the pattern (as seen in the previous example). Similarly, if a pattern matches a key as nil, the pattern will match if the target map either does not contain the key or if the key is associated with the value nil.

If a key in the pattern map is of a simple type, the key will be gotten using the get function. If the pattern map contains keys that are IFns, they will be invoked with the target map as a parameter. Thus, a keyword key will be invoked on the target, and not gotten. Furthermore, any other possible function will be applied to the target, and not gotten in the standard sense.

This illuminates some fascinating possibilities for us; we can use functions as pattern keys to get a more holistic look on our maps. Beyond this, we can apply pattern maps to non maps targets in exciting ways.

```clojure
(matches? {:key 1 #(count (keys %)) 1} {:key 1}) ;=> true

(matches? {pos? false neg? false} 0) ;=> true
```

## Set patterns

Sets work differently than all of our previous patterns. Given an atomic value matching against a set, motif checks that at least one of the set's patterns matches the given target. Given a seqable target, motif checks that all values in the collection match the set.

```clojure
(matches? #{1 2 3} 1) ;=> true

(matches? #{1 2} [1 1 1 2 2]) ;=> true

(matches? #{1} [1 2]) ;=> false
```

Strings, when matched against set patterns, are not considered seqable. Simply put, any functionality gained if we did consider them seqable, is covered by a very simple regex.

```clojure
; *** If we considered strings seqable against sets, which we don't ***
(matches? #{\a \b} "aaaabbaaba") ;=> true (not really)
; easily covered by
(matches? #"(a|b)*" "aaaabbaaba") ;=> true
; *** Thus, we consider them non-seqable as follows ***
(matches? #{"asdf" "qwer" "zxcv"} "asdf") ;=> true

(matches? #{\a \b} "aaaabbaaba") ;=> false
```

Let's see a useful combination of our map and set patterns. Let's see how we could check if a map matches a pattern and additionally only contains a given set of keys.

```clojure
(matches? {:x 1 :y 2 keys #{:x :y}} {:x 1 :y 2}) ;=> true

(matches? {:x 1 :y 2 keys #{:x :y}} {:x 1 :y 2 :z 3}) ;=> false
```

## Logic patterns

Fantastically enough, we need not introduce any new magic in this section! Everything we need for logical disjunctions (or) and conjunctions (and) is already at our finger tips. In fact, we just went over them: map serve as our ands, and sets as our ors.

Let's take a look: we saw that with maps, each key must satisfy its associated pattern. However, as we can use function as our keys, the and pattern simply falls out for us.

```clojure
(matches? {pos? false neg? false} 0) ;=> true

(matches? {even? true neg? true} -3) ;=> false
```

Great! Onto sets and ors: sets require that the target matches one of its contained patterns, just as an or statement.

```clojure
(matches? #{pos? neg?} -1) ;=> true

(matches? #{pos? neg?} 0) ;=> false
```

## Some things to note

### Non-symmetry
matches? is not symmetric! That is (matches? a b) does not imply (matches? b a). This is due to patterns being treated differently that their targets. Consider the following:
```clojure
(matches? [\a \b] "ab") ;=> true

(matches? "ab" [\a \b]) ;=> false
```

A vector pattern will attempt to seq its target, returning false if it can't. In the first case, the string "ab" is seqable as a character vector and matches the pattern. However, as discussed before, string patterns are treated as atomic values, and thus equality is checked. As (= "ab" [\a \b]) is false, the pattern does not match

### When collection patterns get in the way

Sometimes, you want to treat collections as atomic values, checking equality opposed to using their special function in motif. In these cases, remember how functions work! Instead of using the collection as the pattern, explicitly check for equality yourself:

```clojure
(def m1 {:x 1 :y 2 str "{:x 1 :y 2}"})
(def m2 {:x 1 :y 2})

(matches? m1 m2) ;=> true

(matches? m1 m1) ;=> false

(matches? (partial = m1) m1) ;=> true
```

## That's it!

That's all you need to go on into the world. But before you go, let's look at some fun examples that may come in handy some day:

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
