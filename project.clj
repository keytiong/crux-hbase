(defproject io.kosong.crux/crux-hbase-dev "0.1.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]
            [nrepl/lein-nrepl "0.3.2"]]
  :packaging "pom"

  :source-paths ^:replace ["dev"]

  :profiles {:repl {
                    :dependencies [[io.kosong.crux/crux-hbase :version]
                                   [io.kosong.crux/crux-hbase-embedded :version]
                                   [org.clojure/clojure]
                                   [integrant "0.8.0"]
                                   [integrant/repl "0.3.1"]]}
             :dev [:repl]
             :nrepl {:dependencies [[nrepl "0.8.3"]
                                    [clojure-complete "0.2.5"]]}}

  :modules {:parent "crux-hbase-parent"
            :dirs ["crux-hbase" "crux-hbase-embedded" "crux-hbase-test"]}

  :aliases {"clean" ["modules" "clean"]
            "build" ["do" ["modules" "install"] ["modules" "test"]]
            "test"  ["modules" "test"]})
