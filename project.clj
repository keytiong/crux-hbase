(defproject io.kosong.crux/crux-hbase-dev "0.1.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]
            [nrepl/lein-nrepl "0.3.2"]]
  :packaging "pom"

  :profiles {:dev {:source-paths ^:replace ["dev"]
                   :dependencies [[nrepl "0.8.2"]]}}

  :modules {:parent "crux-hbase-parent"
            :dirs ["crux-hbase" "crux-hbase-embedded" "crux-hbase-test"]}

  :dependencies [[io.kosong.crux/crux-hbase "0.1.0-SNAPSHOT"]
                 [io.kosong.crux/crux-hbase-embedded "0.1.0-SNAPSHOT"]
                 [org.clojure/clojure "1.10.1"]
                 [integrant "0.8.0"]
                 [integrant/repl "0.3.1"]]

  :aliases {"clean" ["modules" "clean"]
            "build" ["do" ["modules" "install"] ["modules" "test"]]
            "test"  ["modules" "test"]})
