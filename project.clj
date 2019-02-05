(defproject motif "0.1.0"
  :description "Recursive, data driven, pattern matching for Clojure"
  :url "https://github.com/invocatis/motif"

  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url "https://www.gnu.org/licenses/gpl.txt"}

  :dependencies [[org.clojure/clojure "1.9.0"]]

  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]

  :target-path "target/%s"

  :profiles
    {:uberjar {:aot :all}
     :dev {:dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]
                          [criterium "0.4.4"]]}})
