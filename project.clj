(defproject io.kosong.crux/crux-hbase-dev "0.3.0"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]
            [nrepl/lein-nrepl "0.3.2"]]
  :packaging "pom"

  :source-paths ^:replace ["dev"
                           "crux-hbase/src/main/clojure"
                           "crux-hbase-embedded/src/main/clojure"
                           "crux-hbase-test/src/main/clojure"]

  :resource-paths ^:replace ["dev-resources"]

  :profiles {:repl {:dependencies [[io.kosong.crux/crux-hbase :version]
                                   [io.kosong.crux/crux-hbase-embedded :version]
                                   [org.clojure/clojure]
                                   [integrant "0.8.0"]
                                   [integrant/repl "0.3.1"]
                                   [org.clojure/tools.namespace "1.1.0"]
                                   [nrepl "0.8.3"]
                                   [clojure-complete "0.2.5"]
                                   [ch.qos.logback/logback-classic "1.2.3"]]}
             :dev [:repl]}

  :modules {:parent "crux-hbase-parent"
            :dirs ["crux-hbase" "crux-hbase-embedded" "crux-hbase-test"]}

  :exclusions [org.slf4j/slf4j-log4j12]

  :aliases {"clean" ["modules" "clean"]
            "build" ["do" ["modules" "install"] ["modules" "test"]]
            "test"  ["modules" "test"]})
