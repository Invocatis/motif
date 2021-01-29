(ns motif.macro)


(defmulti as-macro
  (fn [form]
    (if (vector? form)
      (first form)
      form)))

(defmethod as-macro :and
  [[_ & args]]
  `(and ~@args))

(defmethod as-macro :or
  [[_ & args]]
  `(or ~@args))

(defmethod as-macro :apply
  [[_ & args]]
  args)

(defmethod as-macro :access
  [[_ target {:keys [accessor]}]]
  (if (empty? accessor)
    target
    `(get-in ~target ~accessor)))

(defmethod as-macro :default
  [form]
  form)

(defn to-macro
  [form]
  (if (vector? form)
    (as-macro (vec (cons (first form) (map to-macro (rest form)))))
    form))
