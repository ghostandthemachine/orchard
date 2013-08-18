(defproject thinker "0.1.0-SNAPSHOT"
  :description "Organize your thoughts and things."
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1820"]
                 [org.clojure/core.async "0.1.0-SNAPSHOT"]
                 [com.cemerick/clojurescript.test "0.0.5-SNAPSHOT"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/data.json "0.2.1"]
                 [crate "0.2.4"]
                 [node-webkit-cljs "0.1.4"]]

  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}

  :plugins [[lein-cljsbuild "0.3.0"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:id "whitespace"
                        :source-paths ["src"]
                        :notify-command ["growlnotify" "-m"]
                        :compiler {:output-to "public/js/thinker.js"
                                   :optimizations :whitespace
                                   :warnings      true
                                   :pretty-print  true}}

                       {:id "advanced"
                        :source-paths ["src"]
                        :notify-command ["growlnotify" "-m"]
                        :compiler {:output-to "public/js/thinker.js"
                                   :optimizations :advanced
                                   :warnings      true
                                   :pretty-print  false
                                   :externs ["resources/externs.js"]}}

                       {:id "test"
                        :source-paths ["test" "src"]
                        :compiler {:output-to "public/js/tests.js"
                                   :optimizations :whitespace
                                   :warnings      true
                                   :pretty-print  true}}]
              :test-commands {"unit-tests" ["./think" "-test"]}})
