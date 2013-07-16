(defproject async-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.match "0.2.0-rc2"]
                 [core.async "0.1.0-SNAPSHOT"]]

  :source-paths ["clojurescript/src/clj"
                 "clojurescript/src/cljs"]

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
                :output-to "timers.js"}}
    {:id "xhr"
     :source-paths ["src/async_test/xhr"
                    "src/async_test/utils"]
     :compiler {:optimizations :whitespace
                :pretty-print true
                :static-fns true
                :output-to "xhr.js"}}
     ]})
