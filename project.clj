(defproject async-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1845-RC1"]
                 [org.clojure/core.match "0.2.0-rc2"]
                 [core.async "0.1.0-SNAPSHOT"]]
  
  :repositories {"sonatype-staging"
                 "https://oss.sonatype.org/content/groups/staging/"}

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild
  {:builds
   [{:id "mouse"
     :source-paths ["src/async_test/mouse"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "mouse.js"}}
    {:id "daisy"
     :source-paths ["src/async_test/daisy"
                    "src/async_test/utils"]
     :compiler {:optimizations :simple
                :pretty-print false
                :output-to "daisy.js"}}
    {:id "timeout"
     :source-paths ["src/async_test/timeout"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "timeout.js"}}
    {:id "throttle"
     :source-paths ["src/async_test/throttle"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "throttle.js"}}
    {:id "debounce"
     :source-paths ["src/async_test/debounce"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "debounce.js"}}
    {:id "robpike"
     :source-paths ["src/async_test/robpike"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "robpike.js"}}
    {:id "autocomplete"
     :source-paths ["src/async_test/autocomplete"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "autocomplete.js"}}
    {:id "binding"
     :source-paths ["src/async_test/binding"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "binding.js"}}
    {:id "timers"
     :source-paths ["src/async_test/timers"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "timers.js"}}]})
