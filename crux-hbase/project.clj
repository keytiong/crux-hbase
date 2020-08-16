(defproject io.kosong.crux/crux-hbase "0.1.0-SNAPSHOT"
  :description "Crux HBase"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[juxt/crux-core]
                 [org.apache.hbase/hbase-common :exclusions [net.minidev/json-smart]]
                 [org.apache.hbase/hbase-client :exclusions [net.minidev/json-smart]]]

  :source-paths ^:replace ["src/main/clojure"]
  :java-source-paths ^:replace ["src/main/java"]
  :test-paths ^:replace ["src/test/clojure"])
