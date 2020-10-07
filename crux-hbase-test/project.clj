(defproject io.kosong.crux/crux-hbase-test "0.1.0-SNAPSHOT"
  :description "Crux HBase test module"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}

  :profiles {:dev {:resource-paths ["src/test/resources"]}}

  :dependencies [[io.kosong.crux/crux-hbase :version]
                 [io.kosong.crux/crux-hbase-embedded :version]
                 [juxt/crux-core]
                 [org.clojure/test.check "0.10.0"]])
