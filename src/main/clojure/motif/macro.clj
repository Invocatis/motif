(ns motif.macro
  (:require
   [motif.syntax :as syntax]))

(defmulti as-macro
  (fn [form]
    (if (-> form meta :motif.core/syntax?)
      (first form)
      form)))

(defmethod interpret* :if
  [[_ pred truthy falsey]]
  `(if ~pred ~truthy ~falsey))

(defmethod interpret* :and
  [[_ & args]]
  `(and ~@args))

(defmethod interpret* :or
  [[_ & args]]
  `(or ~@args))

(defmethod interpret* :xor
  [[_ & args]]
  `(= 1 (count (filter boolean ~args))))

(defmethod interpret* :apply
  [[_ f & args]]
  args)

(defmethod interpret* :recur
  [[_ form]])

(defmethod interpret* :access
  [[_ target {:keys [accessor]}]])

(defmethod interpret* :unify
  [[_ sym target {:keys [accessor]}]])

(defmethod interpret* nil
  [_])

(defmethod interpret* :default
  [form])


(defn to-macro
  [pattern result]
  (as-macro (vec (cons (first form) (map to-macro (rest form))))))
