(defproject io.kosong.crux/crux-hbase-dev "0.1.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"

  :profiles {:dev {:source-paths ["dev"]}}

  :modules {:parent "crux-hbase-parent"
            :dirs ["crux-hbase" "crux-hbase-embedded" "crux-hbase-test"]}

  :dependencies [[io.kosong.crux/crux-hbase]
                 [io.kosong.crux/crux-hbase-embedded]
                 [org.clojure/clojure]
                 [integrant "0.8.0"]
                 [integrant/repl "0.3.1"]]

  :aliases {"clean" ["modules" "clean"]
            "build" ["modules" "compile"]
            "test"  ["modules" "test"]})
