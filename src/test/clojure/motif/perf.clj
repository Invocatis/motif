(ns motif.perf
  (:require
   [criterium.core :as crit]
   [motif.core :as motif]))

; (defn simple
;   []
;   (crit/quick-bench
;    (motif/matches?
;     {:a [pos? 2 4]
;      :b {:c #{1 2 3}}
;      :d "asdf"
;      :e ^:| [pos? even?]}
;     {:a [1 2 4]
;      :b {:c 1}
;      :d "asdf"
;      :e [1 1]}))
;   (crit/quick-bench
;    (motif/match
;     {:a [pos? 2 4]
;      :b {:c #{1 2 3}}
;      :d "asdf"
;      :e ^:| [pos? even?]}
;     {:a [1 2 4]
;      :b {:c 1}
;      :d "asdf"
;      :e [1 1]})))
