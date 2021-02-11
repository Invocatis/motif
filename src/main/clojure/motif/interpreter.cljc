(ns motif.interpreter
  (:require
   [motif.syntax :as syntax]))

(syntax/init!)

(defmulti interpret*
  (fn [form environment continuation]
    (cond
      (nil? form)                       nil
      (:motif.core/syntax? (meta form)) (first form)
      :else                             :default)))

(defmethod interpret* :if
  [[_ pred truthy falsey] environment continuation]
  (if (interpret* pred environment continuation)
    (interpret* truthy environment continuation)
    (interpret* falsey environment continuation)))

(defmethod interpret* :and
  [[_ & args] environment continuation]
  (if-not (seq args)
    true
    (and
     (interpret* (first args) environment (into #[:and] (rest args)))
     (interpret* continuation environment nil))))

(defmethod interpret* :or
  [[_ & args] environment continuation]
  (and
   (loop [args args]
     (if (empty? args)
       false
       (if (interpret* (first args) environment nil)
         true
         (recur (rest args)))))
   (interpret* continuation environment nil)))

(defmethod interpret* :xor
  [[_ & args] environment continuation]
  (and
   (= 1 (count (filter true? (map #(interpret* % environment nil) args))))
   (interpret* continuation environment nil)))

(defmethod interpret* :apply
  [[_ f & args] environment continuation]
  (let [value (apply f
               (map
                (fn [form]
                  (if (nil? form)
                    nil
                    (interpret* form environment nil)))
                args))]
    (and value
         (interpret* continuation environment nil)
         value)))

(defmethod interpret* :recur
  [[_ form] environment continuation]
  (interpret* (interpret* form environment nil) environment continuation))

(defn access
  [target path]
  (if (empty? path)
    target
    (reduce
     (fn [form val]
       (cond
         (fn? val) (apply val [form])
         (or (indexed? form) (list? form)) (nth form val nil)
         (associative? form) (get form val)
         :else     (get form val)))
     target
     path)))

(defmethod interpret* :access
  [[_ target path] environment continuation]
  (access (interpret* target environment continuation) path))

(defmethod interpret* :unify
  [[_ sym target accessor] environment continuation]
  (let [value (access target accessor)]
    (if (contains? environment sym)
      (and
       (= (get environment sym) value)
       (interpret* continuation (assoc environment sym value) nil))
      (interpret* continuation (assoc environment sym value) nil))))

(defmethod interpret* nil
  [_ environment continuation]
  (if continuation
    (interpret* continuation environment nil)
    true))

(defmethod interpret* :default
  [form environment continuation]
  (and (interpret* continuation environment nil) form))

(def ^:dynamic *throw-exceptions* false)

(defn interpret
  [form]
  (try
    (interpret* form {} nil)
    (catch Exception e
      (if *throw-exceptions*
        (throw e)
        false))))
