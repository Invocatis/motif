(ns motif.util)

(defn conjunction
  ([f] f)
  ([f g] #(or (f %) (g %)))
  ([f g & more] (reduce conjunction (conjunction f g) more)))

(defn disjunction
  ([f] f)
  ([f g] #(and (f %) (g %)))
  ([f g & more] (reduce disjunction (disjunction f g) more)))
