(defproject io.kosong.crux/crux-hbase-dev "0.1.0-SNAPSHOT"
  :description "Crux HBase development module"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure]
                 [io.kosong.crux/crux-hbase-embedded :version]
                 [juxt/crux-core]
                 [juxt/crux-rocksdb :crux]
                 [juxt/crux-kafka :crux]]
  :profiles {:dev {:source-paths ["dev"]}})
