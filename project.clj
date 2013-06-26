(defproject thinker "0.1.0-SNAPSHOT"
  :description "Organize your thoughts and things."
  :min-lein-version "2.0.0"
  :dependencies [[node-webkit-cljs "0.1.4"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/data.json "0.2.1"]
                 [org.bodil/redlobster "0.2.0"]
                 [crate "0.2.4"]
                 ; [com.keminglabs/c2 "0.2.2"]
                 [com.cemerick/clojurescript.test "0.0.4"]
                 [org.clojure/clojurescript "0.0-1586"]]

  :plugins [[lein-cljsbuild "0.3.0"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:source-paths ["src" "test"]
                        :compiler {:output-to "public/js/thinker.js"
                                   :optimizations :whitespace
                                   :warnings      true
                                   :pretty-print  true}}]
              :test-commands {"unit-tests" ["runners/phantom.js" "public/js/thinker.js"]}})
 