(defproject thinker "0.1.0-SNAPSHOT"
  :description "Organize your thoughts."
  :min-lein-version "2.0.0"

  :dependencies [[node-webkit-cljs "0.1.4"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/data.json "0.2.1"]
                 [org.bodil/redlobster "0.2.0"]
                 ; [com.keminglabs/c2 "0.2.2"]
                 [crate "0.2.4"]]

  :plugins [[lein-cljsbuild "0.3.2"]]

  :cljsbuild {:builds [{:source-paths ["src"
  ;                                     "compiler/clojurescript/src/clj"
  ;                                     "compiler/clojurescript/src/cljs"
                                       ]
                        :notify-command ["growlnotify" "-m"]
                        ;:incremental false ; https://github.com/emezeske/lein-cljsbuild/issues/181
                        :compiler {:output-to     "public/js/thinker.js"
                                   :optimizations :whitespace
                                   :warnings true
                                   :pretty-print true}}]})
