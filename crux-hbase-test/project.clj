(defproject io.kosong.crux/crux-hbase-test "0.2.0"
  :description "Crux HBase test module"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}

  :resource-paths ["src/test/resources"]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]

  :dependencies [[io.kosong.crux/crux-hbase :version :exclusions [io.netty/netty]]
                 [io.kosong.crux/crux-hbase-embedded :version :exclusions [io.netty/netty]]
                 [org.clojure/test.check "0.10.0"]])
