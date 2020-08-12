(defproject io.kosong.crux/crux-hbase-dev "0.1.0-SNAPSHOT"
  :description "Crux HBase development module"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure]
                 [io.kosong.crux/crux-hbase-embedded :version]
                 [io.kosong.crux/crux-hbase :version]
                 [juxt/crux-core]
                 [juxt/crux-kafka :crux]
                 [integrant "0.8.0"]
                 [integrant/repl "0.3.1"]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/test.check "0.10.0"]]}})
