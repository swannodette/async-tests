(defproject async-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1835"]
                 [core.async "0.1.0-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild
  {:builds
   [{:id "mouse"
     :source-paths ["src/async_test/mouse"]
     :compiler {:optimizations :simple
                :pretty-print true
                :static-fns true
                :output-to "main.js"}}
    {:id "timeout"
     :source-paths ["src/async_test/timeout"]
     :compiler {:optimizations :simple
                :pretty-print true
                :static-fns true
                :output-to "main.js"}}
    {:id "throttle"
     :source-paths ["src/async_test/throttle"]
     :compiler {:optimizations :simple
                :pretty-print true
                :static-fns true
                :output-to "main.js"}}
    {:id "debounce"
     :source-paths ["src/async_test/debounce"]
     :compiler {:optimizations :simple
                :pretty-print true
                :static-fns true
                :output-to "main.js"}}]})
