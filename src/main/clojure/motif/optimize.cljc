(ns motif.optimize)


(def optimization-patterns
  [['[:if true t f] 't]
   ['[:if false t f] 'f]
   ['[:and & x] '(into [:and] (remove true? x))]
   ['[:or & x] '(into [:or] (remove false? x))]])
