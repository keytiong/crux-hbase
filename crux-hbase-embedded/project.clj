(defproject io.kosong.crux/crux-hbase-embedded "0.1.0-SNAPSHOT"
  :description "Embedded HBase or development and testing"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.apache.hbase/hbase-server :exclusions [net.minidev/json-smart
                                                             org.glassfish/javax.el]]]
  :source-paths ^:replace ["src/main/clojure"]
  :java-source-paths ^:replace ["src/main/java"]
  :test-paths ^:replace ["src/test/clojure"])
