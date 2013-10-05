(defproject async-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1913"]
                 [org.clojure/core.match "0.2.0"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]]

  :plugins [[lein-cljsbuild "0.3.3"]]

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
    {:id "timers"
     :source-paths ["src/async_test/timers"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print false
                :static-fns true
                :output-to "timers.js"}}]})
