(ns motif.interpreter)

(defmulti interpret
  (fn [form environment continuation]
    (cond
      (nil? form)                      nil
      (or (vector? form) (list? form)) (first form)
      :else                            form)))

(defmethod interpret :if
  [[_ pred truthy falsey] environment continuation]
  (if (interpret pred environment continuation)
    (interpret truthy environment continuation)
    (interpret falsey environment continuation)))

(defmethod interpret :and
  [[_ & args] environment continuation]
  (if-not (seq args)
    true
    (and
     (interpret (first args) environment (into [:and] (rest args)))
     (interpret continuation environment nil))))

(defmethod interpret :or
  [[_ & args] environment continuation]
  (and
   (some boolean (map #(interpret % environment nil) args))
   (interpret continuation environment nil)))

(defmethod interpret :xor
  [[_ & args] environment continuation]
  (and
   (= 1 (count (filter true? (map #(interpret % environment nil) args))))
   (interpret continuation environment nil)))

(defmethod interpret :apply
  [[_ f & args] environment continuation]
  (let [value (apply f
               (map
                #(interpret % environment nil)
                args))]
    (and value
         (interpret continuation environment nil)
         value)))

(defmethod interpret :recur
  [[_ form] environment continuation]
  (interpret (interpret form environment nil) environment continuation))

(defn access
  [target accessor]
  (if (empty? accessor)
    target
    (reduce
     (fn [form val]
       (cond
         (fn? val) (apply val [form])
         (seq? form) (nth form val)
         :else     (get form val)))
     target
     accessor)))

(defmethod interpret :access
  [[_ target {:keys [accessor]}] environment continuation]
  (access target accessor))

(defmethod interpret :unify
  [[_ sym target {:keys [accessor]}] environment continuation]
  (let [value (access target accessor)]
    (if (contains? environment sym)
      (and
       (= (get environment sym) value)
       (interpret continuation (assoc environment sym value) nil))
      (interpret continuation (assoc environment sym value) nil))))

(defmethod interpret :constant
  [[_ value] environment continuation]
  (and value
       (interpret continuation environment nil)
       value))

(defmethod interpret nil
  [_ _ _]
  true)

(defmethod interpret :default
  [form environment continuation]
  (and form
       (interpret continuation environment nil)
       form))
