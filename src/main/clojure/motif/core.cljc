(ns motif.core
  (:require
   [motif.compiler :as compiler]
   [motif.interpreter :refer [interpret]]))
   ; [motif.macro :as macro]

(def _ (constantly true))

(defn matches?
  [pattern target]
  (interpret (compiler/compile pattern target)))

; (defmacro match
;   [target & branches]
;   `(cond ~@(mapcat (fn [pattern result] (macro/to-macro (cpm pattern target) result)))))
