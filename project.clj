(defproject thinker "0.1.0-SNAPSHOT"
  :description "Organize your thoughts and things."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1820"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]
                 [com.cemerick/clojurescript.test "0.0.4"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/data.json "0.2.1"]
                 [org.bodil/redlobster "0.2.0"]
                 [crate "0.2.4"]
                 [node-webkit-cljs "0.1.4"]]

  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}

  :plugins [[lein-cljsbuild "0.3.0"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:source-paths ["src" "test"]
                        :compiler {:output-to "public/js/thinker.js"
                                   :notify-command ["growlnotify" "-m" "%"]
                                   :optimizations :whitespace
                                   :warnings      true
                                   :pretty-print  true}}

                       {:source-paths ["test" "src"]
                        :compiler {:output-to "public/js/tests.js"
                                   :optimizations :whitespace
                                   :warnings      true
                                   :pretty-print  true}}]
              :test-commands {"unit-tests" ["./think" "-test"]}})
