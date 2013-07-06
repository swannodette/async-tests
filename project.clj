(defproject async-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1835"]
                 [org.clojure/core.match "0.2.0-rc2"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]]

  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public"}

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild
  {:builds
   [{:id "mouse"
     :source-paths ["src/async_test/mouse"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "main.js"}}
    {:id "timeout"
     :source-paths ["src/async_test/timeout"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "main.js"}}
    {:id "throttle"
     :source-paths ["src/async_test/throttle"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "main.js"}}
    {:id "debounce"
     :source-paths ["src/async_test/debounce"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "main.js"}}
    {:id "robpike"
     :source-paths ["src/async_test/robpike"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "main.js"}}
    {:id "bench"
     :source-paths ["src/async_test/bench"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "main.js"}}
    {:id "autocomplete"
     :source-paths ["src/async_test/autocomplete"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "autocomplete.js"}}]})
