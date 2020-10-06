(defproject io.kosong.crux/crux-hbase-test "0.1.0-SNAPSHOT"
  :description "Crux HBase test module"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}
  :test-paths ["src/test/clojure"]

  :profiles {:dev {:dependencies [[io.kosong.crux/crux-hbase]
                                  [io.kosong.crux/crux-hbase-embedded]
                                  [org.clojure/test.check "0.10.0"]
                                  [org.slf4j/log4j-over-slf4j "1.7.30"]
                                  [ch.qos.logback/logback-classic "1.2.3"]]
                   :resource-paths ["src/test/resources"]}}

  ;;:dependencies [[io.kosong.crux/crux-hbase :version]
  ;;               [io.kosong.crux/crux-hbase-embedded :version]
  ;;               [juxt/crux-core]
  ;;               [org.clojure/test.check "0.10.0"]]
  )
