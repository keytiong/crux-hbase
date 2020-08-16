(defproject io.kosong.crux/crux-hbase-test "0.1.0-SNAPSHOT"
  :description "Crux HBase test module"
  :plugins [[lein-modules "0.3.11"]]

  :source-paths ^:replace ["src/main/clojure"]
  :java-source-paths ^:replace ["src/main/java"]
  :test-paths ^:replace ["src/test/clojure"]

  :profiles {:dev {:dependencies [[io.kosong.crux/crux-hbase-embedded :version]
                                  [io.kosong.crux/crux-hbase :version]
                                  [juxt/crux-core]
                                  [org.clojure/test.check "0.10.0"]]}})
