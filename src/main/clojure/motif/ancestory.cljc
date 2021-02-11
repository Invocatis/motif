(ns motif.ancestory)

(defn eldest
  ([anc]
   (when anc
     (:parent anc anc)))
  ([anc key]
   (when anc
     (or (eldest (:parent anc) key)
         (get anc key)))))

(defn youngest
  [anc key]
  (when anc
    (if (contains? anc key)
      (get anc key)
      (recur (:parent anc) key))))

(defn lineage
  [anc key]
  (if anc
     (if-let [v (get anc key)]
       (conj (lineage (:parent anc) key) v)
       (lineage (:parent anc) key))
    []))
