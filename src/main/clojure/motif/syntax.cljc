(ns motif.syntax
  (:import
   (clojure.lang LispReader)))

(def dispatch-macros
  (let [dm (.getDeclaredField LispReader "dispatchMacros")
        _  (.setAccessible dm true)]
    (.get dm nil)))

(defn set-dispatch-macro-character!
  [character read]
  (aset dispatch-macros (int character) read))

(defn init!
  []
  (set-dispatch-macro-character! \[
    (fn [reader quote opts pending-forms]
      (with-meta (vec (LispReader/readDelimitedList \] reader false opts pending-forms)) {:motif.core/syntax? true}))))
