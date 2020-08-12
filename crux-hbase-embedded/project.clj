(defproject io.kosong.crux/crux-hbase-embedded "0.1.0-SNAPSHOT"
  :description "Embedded HBase or development and testing"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure]
                 [org.apache.hbase/hbase-server :exclusions [net.minidev/json-smart
                                                             org.glassfish/javax.el]]]

  :java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"])
